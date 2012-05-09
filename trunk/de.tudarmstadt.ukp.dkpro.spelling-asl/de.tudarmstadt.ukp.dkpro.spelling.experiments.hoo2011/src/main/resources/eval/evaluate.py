# File:     evaluate.py
#
# Author:	George Narroway 
# Modified:	15 August 2011
# Version:      2.31
#
# Copyright (c) Centre for Language Technology, Macquarie University
# Not to be used for commercial purposes.
#
# Purpose:
# Given two two files of edits in an xml representation (gold and system),
# evaluates the system edits against the gold.
#
# Usage:
# Print out evaluation statistics:
# python evaluate.py <gold.xml> <system.xml>
#
# Result of each (i)ndividual gold edit:
# python evaluate.py -i <gold.xml> <system.xml>

from difflib import *
import re
import sys
import os
import xml.etree.cElementTree as et
from optparse import OptionParser


VERBOSE = False
OPTIONAL_BONUS = True


class Edit:
    index = ""
    start = 0
    end = 0
    tag = ""
    original = ""
    corrections = []

    def get_overlap(self, edit):
        """Calculates the degree of extent overlap between an edit and another.

        returns: a float between 0.00 and 1.00
        """
        #print "gold:", self.to_string()
        #print "sys:", edit.to_string()
        
        if self.start != self.end:
            # Gold is replacement or deletion
            if edit.start == edit.end:
                # Sys is an insertion
                if (self.start < edit.start) and (self.end > edit.end):
                    overlap = float(1) / (self.end - self.start)
                else:
                    overlap = 0                    
            else:
                # Sys is replacement or deletion
                r1 = range(self.start, self.end)
                r2 = range(edit.start, edit.end)
                overlap = float(len(set(r1).intersection(r2))) / len(set(r1).union(r2))
        else:
            # Gold is insertion
            if (edit.start == self.start) and (edit.end == self.end):
                # Sys insertion at same offset
                overlap = 1 # recognized
            elif (edit.start < self.start) and (edit.end > self.end):
                # Sys edit envelops gold insertion
                overlap = float(1) / (edit.end - edit.start) # detected
            elif (edit.start < self.start) and (edit.end == self.end):
                # Case B2
                if not self.corrections:
                    overlap = float(1) / (edit.end - edit.start)
                else:
                    # Tail string is the same
                    try:
                        matches = [1 for c in self.corrections if c and edit.corrections[0].endswith(c)]
                    except IndexError:
                        matches = 0 # Sys proposes no corrections

                    overlap = float(bool(matches)) / (edit.end - edit.start)
            elif (edit.start == self.start) and (edit.end > self.end):
               # Case B3
                if not self.corrections:
                    overlap = float(1) / (edit.end - edit.start)
                else:
                    # Start string is the same
                    try:
                        matches = [1 for c in self.corrections if c and edit.corrections[0].startswith(c)]
                    except IndexError:
                        matches = 0 # System proposes no corrections

                    overlap = float(bool(matches)) / (edit.end - edit.start)
                    
            else:
                overlap = 0
                
        return overlap


    def is_optional(self):
        """An optional edit has a null correction."""
        return None in self.corrections

    def to_string(self):
        return "%s [%d:%d] (%s): '%s' => %s" % \
               (self.index, self.start, self.end, self.tag,
                self.original, self.corrections)



def get_alignments(e1, edits, mode='lenient'):
    """Filters a list of edits to those that align with an edit e1.
    lenient -- the extent must overlap at least a bit.
    else -- the extent must be a perfect match.

    returns: a list of aligned edits.
    """
    if mode == 'lenient':
        overlaps = [e2 for e2 in edits if e1.get_overlap(e2)]
    else:
        overlaps = [e2 for e2 in edits if e1.get_overlap(e2) == 1]
    
    return overlaps



def make_alignment_sets(gold_edits, sys_edits, mode):
    """Each alignment set is a mapping of an 
    edit to a list of edits that align with it.

    returns: gold and system alignment sets within a single dictionary.
    """
    alignments = dict()
    alignments['gold'] = dict((ge, get_alignments(ge, sys_edits, mode))
                              for ge in gold_edits)
    alignments['sys'] = dict((se, get_alignments(se, gold_edits, mode))
                             for se in sys_edits)

    return alignments



def has_matching_gold(sys_edit, gold_edits):
    """Checks whether any gold edit in a list (gold_edits)
    has the same 'corrected' string has an edit (sys_edit). This
    is used for regular scoring of correction in score_correction().

    sys_edit -- a system edit with >= 0 corrections
    gold_edits -- list of >=1 gold edits with >= 0 corrections.
    
    returns: a boolean.
    """    
    for gold_edit in gold_edits:
        # Case 1: Gold edit proposes no corrections
        if not gold_edit.corrections:
            return True

        # Case 2: Iteratively try and find a physical match
        for gold_corr in gold_edit.corrections:
            try:
                if gold_corr == sys_edit.corrections[0]:
                    return True
            except IndexError:
                # System didn't propose a correction, valid only if gold is optional
                if gold_edit.is_optional():
                    return True
                else:
                    print "The system didn't propose a correction."
                    print "Gold:", gold_edit.to_string()
                    print "System:", sys_edit.to_string()                  

    return False



def has_matching_sys(gold_edit, sys_edits):
    """Checks whether any system edits in a list (sys_edits)
    has the same 'corrected' string has a gold edit.
    This is used for scoring correction in score_by_type().

    gold_edit -- a gold edit with >= 0 corrections
    sys_edits -- list of >= 0 sys edits with >= 0 corrections.
    
    returns: a boolean.
    """   
    # Case 1: Gold edit proposes no corrections
    if not gold_edit.corrections:
        return True
    
    # Case 2: Iteratively try and find a physical match
    for sys_edit in sys_edits:
        for gold_corr in gold_edit.corrections:
            try:
                if gold_corr == sys_edit.corrections[0]:
                    return True
            except IndexError:
                # System didn't propose a correction, valid only if gold is optional
                if gold_edit.is_optional():
                    return True
                else:            
                    print "The system didn't propose a correction."
                    print "Gold:", gold_edit.to_string()
                    print "System:", sys_edit.to_string()

    return False
        


def calculate_Fscore(precision, recall):
    """Calculates the F-score given precision and recall values."""
    try:
        return 2 * ((precision * recall) / (precision + recall))
    except ZeroDivisionError:
        return 0



def score_detection(alignments):
    """Calculates the Precision, Recall and DetectionScore for detection.

    alignments -- lenient alignment sets.
    
    returns: A dictionary of each calculated statistic.
    """   
    # Number of gold edits with at least one aligned system edit
    detected = sum(1 for v in alignments['gold'].itervalues() if v)

    # Number of optional gold edits that weren't detected
    missed_optional = sum(1 for k,v in alignments['gold'].iteritems()
                          if k.is_optional() and not v)

    # Make missed_optional = 0 if OPTIONAL_BONUS is False
    global OPTIONAL_BONUS
    missed_optional *= OPTIONAL_BONUS

    # Number of system edits that don't align
    spurious = sum(1 for v in alignments['sys'].itervalues() if not v)

    
    
    try:
        precision = float(detected + missed_optional) / \
                    (detected + spurious + missed_optional)
    except ZeroDivisionError:
        precision = 0

    try:
        recall = float(detected + missed_optional) / len(alignments['gold'])
    except ZeroDivisionError:
        recall = 0

    score = calculate_Fscore(precision, recall)

    if VERBOSE:
        print "\nScoring detection..."
        print "  Detected edits:", detected
        print "  Missed optional edits:", missed_optional
        print "  Spurious edits:", spurious
        print "  Gold edits:", len(alignments['gold'])
        print "- Precision: (%d + %d) / (%d + %d + %d) = %.2f" % \
              (detected, missed_optional,
               detected, spurious, missed_optional, precision)
        print "- Recall: (%d + %d) / %d = %.2f" % \
              (detected, missed_optional, len(alignments['gold']), recall)
        print "- Score: %.2f" % score
    
    return dict(precision=precision, recall=recall, score=score)



def score_recognition(strict_aligns, lenient_aligns):
    """Calculates the Precision, Recall and Score for recognition.
    
    alignments -- strict alignment sets.

    returns: A dictionary of each calculated statistic.
    """
    # Number of gold edits with at least one aligned system edits
    recognised = sum(1 for v in strict_aligns['gold'].itervalues() if v)

    # Number of optional gold edits that weren't recognised
    missed_optional = sum(1 for k,v in lenient_aligns['gold'].iteritems()
                          if k.is_optional() and not v)

    # Make missed_optional = 0 if OPTIONAL_BONUS is False
    global OPTIONAL_BONUS
    missed_optional *= OPTIONAL_BONUS
    
    try:
        precision = float(recognised + missed_optional) / \
                    (len(strict_aligns['sys']) + missed_optional)
    except ZeroDivisionError:
        precision = 0

    try:
        recall = float(recognised + missed_optional) / len(strict_aligns['gold'])
    except ZeroDivisionError:
        recall = 0

    score = calculate_Fscore(precision, recall)

    if VERBOSE:
        print "\nScoring recognition..."
        print "  Recognised edits:", recognised
        print "  Missed optional edits:", missed_optional
        print "  Gold edits:", len(strict_aligns['gold'])
        print "  System edits:", len(strict_aligns['sys'])
        print "- Precision: (%d + %d) / (%d + %d) = %.2f" % \
              (recognised, missed_optional,
               len(strict_aligns['sys']), missed_optional, precision)
        print "- Recall: (%d + %d) / %d = %.2f" %\
              (recognised, missed_optional, len(strict_aligns['gold']), recall)
        print "- Score: %.2f" % score

    return dict(precision=precision, recall=recall, score=score)



def score_correction(strict_aligns, lenient_aligns):
    """Calculates the Precison, Recall and Score for correction, where
    a perfect correction has a strictly aligned and the correction string
    is identical.
    
    alignments -- strict alignment sets.

    returns: A dictionary of each calculated statistic.
    """    
    # System edits that have a strict extent match with at least 1 gold edit
    aligned_sys_edits = set([k for k,v in strict_aligns['sys'].iteritems() if v])

    # Number of aligned edits that string match one of their aligned gold edits
    valid_corrections = sum(1 for e in aligned_sys_edits
                            if has_matching_gold(e, strict_aligns['sys'][e]))

    # Number of optional gold edits that weren't recognised
    missed_optional = sum(1 for k,v in lenient_aligns['gold'].iteritems()
                          if k.is_optional() and not v)

    # Make missed_optional = 0 if OPTIONAL_BONUS is False
    global OPTIONAL_BONUS
    missed_optional *= OPTIONAL_BONUS
    
    try:
        precision = float(valid_corrections + missed_optional) / \
                    (len(strict_aligns['sys']) + missed_optional)
    except ZeroDivisionError:
        precision = 0
        
    try:
        recall = float(valid_corrections + missed_optional) / len(strict_aligns['gold'])
    except ZeroDivisionError:
        recall = 0

    score = calculate_Fscore(precision, recall)

    if VERBOSE:
        print "\nScoring corrections..."
        print "  Aligned system edits:", len(aligned_sys_edits)
        print "  Valid corrections:", valid_corrections
        print "  Missed optional edits:", missed_optional
        print "  Gold edits:", len(strict_aligns['gold'])
        print "  System edit:", len(strict_aligns['sys'])
        print "- Precision: (%d + %d) / (%d + %d) = %.2f" % \
              (valid_corrections, missed_optional,
               len(strict_aligns['sys']), missed_optional, precision)
        
        print "- Recall: (%d + %d) / %d = %.2f" % \
              (valid_corrections, missed_optional, len(strict_aligns['gold']), recall)
        print "- Score: %.2f" % score

    return dict(precision=precision, recall=recall, score=score)
    


def score(gold_edits, sys_edits):
    """Scores two lists of edits against each other."""
    lenient_alignments = make_alignment_sets(gold_edits, sys_edits, "lenient")
    strict_alignments = make_alignment_sets(gold_edits, sys_edits, "strict")

    detection = score_detection(lenient_alignments)
    recognition = score_recognition(strict_alignments, lenient_alignments)
    correction = score_correction(strict_alignments, lenient_alignments)
    
    return dict(detection=detection, recognition=recognition, correction=correction)



def score_by_type(gold_edits, sys_edits, mode="total"):
    """Scores two lists of edits, grouping by type.
    Returns: dict of tag: (count, detections, recognitions, corrections).""" 
    count = dict()
    detections = dict()
    recognitions = dict()
    corrections = dict()

    if mode == "required":
        gold_edits = [e for e in gold_edits if not e.is_optional()]
    elif mode == "optional":
        gold_edits = [e for e in gold_edits if e.is_optional()]
    else:
        pass # mode = "total"

    for g in gold_edits:
        # Tag totals
        count[g.tag] = count.get(g.tag, 0) + 1

        # Detections by tag
        for s in sys_edits:
            if g.get_overlap(s):
                detections[g.tag] = detections.get(g.tag, 0) + 1
                break

        # System edits that have a strict extent match with the gold edit
        strict_aligned_sys = get_alignments(g, sys_edits, mode='strict')

        if strict_aligned_sys:
            # Recognised if at least one strictly aligned system edit
            recognitions[g.tag] = recognitions.get(g.tag, 0) + 1
        
            # Correct if it has a match in the system
            if has_matching_sys(g, strict_aligned_sys):
                corrections[g.tag] = corrections.get(g.tag, 0) + 1
        '''else:
            # System edits that have a lenient extent match with the gold edit
            lenient_aligned_sys = get_alignments(g, sys_edits, mode='lenient')

            # Correct if optional and system did nothing
            if g.is_optional() and not lenient_aligned_sys:
                corrections[g.tag] = corrections.get(g.tag, 0) + 1'''

    output = dict()
    
    for k in sorted(count.iterkeys()):
        output[k] = (count[k], detections.get(k, 0),
                     recognitions.get(k, 0), corrections.get(k, 0))

    return output



def parse_edits(edits_file):
    """Parses the XML edits into a list of Edit objects. We
    use objects instead of dictionaries so that they can be used
    as hash keys in gold->[system] mappings later on. Also make
    sure that <empty/> objects are turned into empty strings.

    returns: a list of Edit objects.
    """
    edits = list()
    root = et.parse(edits_file)

    if VERBOSE: print "Parsing edits in %s..." % os.path.basename(edits_file)

    for e in root.getiterator("edit"):
        edit = Edit()
        edit.index = e.get('index')
        edit.start = int(e.get('start'))
        edit.end = int(e.get('end'))
        edit.tag = e.get('type')


        # Replace <empty/> in original with ""
        original_elem = e.find('original')

        if original_elem.find("empty") is None:
            edit.original = original_elem.text
        else:
            edit.original = ""


        # Compile corrections and replace <empty/> tags with ""
        corrections_elem = e.find('corrections')

        if corrections_elem:
            corrections = corrections_elem.findall("correction")
            has_empty = bool(sum(c.find("empty") is not None for c in corrections))

            if has_empty:
                edit.corrections = [c for c in corrections
                                    if c.find("empty") is None]
                edit.corrections.append("")
            else:
                edit.corrections = [c.text for c in corrections]

        # print edit.to_string()                 
        edits.append(edit)

    if VERBOSE: print "Found %d edits" % len(edits)
            
    return edits



def print_scores(scores):
    """A simple printer to print the different types of scores."""
    for k in ("detection", "recognition", "correction"):
        print "\n%s" % k
        for s,v in scores[k].iteritems():
            print "  %s: %.2f" % (s, v)



def process(gold_file, system_file, optional_bonus=True):
    """Processes two files and returns scores for
    detection, recognition and correction."""
    try:
        global OPTIONAL_BONUS
        OPTIONAL_BONUS = optional_bonus
        gold_edits = parse_edits(gold_file)
        system_edits = parse_edits(system_file)
    except Exception as e:
        print "Error parsing xml file: %s" % e

    return score(gold_edits, system_edits)



def process_type(gold_file, system_file, mode="total"):
    """Processes two files and returns scores for
    detection, recognition and correction grouped by type."""

    try:
        gold_edits = parse_edits(gold_file)
        system_edits = parse_edits(system_file)
    except Exception as e:
        print "Error parsing xml file: %s" % e

    return score_by_type(gold_edits, system_edits, mode)



def process_individually(gold_file, system_file):
    try:
        gold_edits = parse_edits(gold_file)
        system_edits = parse_edits(system_file)
    except Exception as e:
        print "Error parsing xml file: %s" % e

    output = list()

    for edit in gold_edits:
        line = list()

        line.append(edit.index)
        line.append(edit.tag)

        detected_edits = get_alignments(edit, system_edits, mode='lenient')

        if detected_edits:
            recognised_edits = get_alignments(edit, detected_edits, mode="strict")

            if recognised_edits:
                if has_matching_sys(edit, recognised_edits):
                    line.append("Corrected")
                else:
                    line.append("Recognised")
            else:
                line.append("Detected")
        else:
            line.append("Missed")

        output.append(",".join(line))

    for line in output:
        print line



def print_usage():
    print """# File:     evaluate.py
#
# Author:	George Narroway 
# Modified:	11 August 2011
# Version:      2.3
#
# Copyright (c) Centre for Language Technology, Macquarie University
# Not to be used for commercial purposes.
#
# Purpose:
# Given two two files of edits in an xml representation (gold and system),
# evaluates the system edits against the gold.
#
# Usage:
# Print out evaluation statistics:
# python evaluate.py <gold.xml> <system.xml>
#
# Result of each (i)ndividual gold edit:
# python evaluate.py -i <gold.xml> <system.xml>
"""
    
        
if __name__ == "__main__":
    parser = OptionParser()
    parser.add_option("-i", action="store_true", dest="eval_individually")
    (options, args) = parser.parse_args()
    
    if len(args) == 2:
        VERBOSE = True
        
        gold_file = args[0]
        system_file = args[1]

        try:
            if not options.eval_individually:
               scores = process(gold_file, system_file, optional_bonus=True)
               print_scores(scores)
            else:
               process_individually(gold_file, system_file)
        except Exception as e:
            print "An error occurred.", e
        else:
            print "End."
    else:
        print_usage()
        sys.exit()


