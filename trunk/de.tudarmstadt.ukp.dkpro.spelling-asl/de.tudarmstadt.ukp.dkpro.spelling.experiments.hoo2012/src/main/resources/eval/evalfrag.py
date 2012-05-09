# File:       evalfrag.py
#
# Author:     George Narroway 
# Modified:   18 April 2012
# Edited by:  Ilya Anisimoff
# Version:    0.81
#
# Changes in Version 0.8
#
# If a gold edit is a deletion and the corresponding system edit is a
# replacement, or vice versa, span matching takes account of one span
# including a space that the other does not; these matches are then
# counted as recognitions rather than as detections.
#
# Changes in Version 0.71
# Added -c option to control case sensitivity of edit matching
# Spurious edits now only shows edits of the specified types
# Gracefully handles situation when a gold index has no edit
#
# Copyright (c) Centre for Language Technology, Macquarie University
# Not to be used for commercial purposes.
#
# Purpose:
# Compare two files containing edit structures.
#
# usage: evalfrag.py [-c {match,nomatch}]
#                    [-h] [-m {prf,p,r,pr}] [-r {bonus,nobonus}] [-t TYPES]
#                    [-o OUTFILE]
#                    goldedits sysedits
#
# positional arguments:
#   goldedits
#   sysedits
#
# optional arguments:
#   -c {match,nomatch}
#   -h, --help            show this help message and exit
#   -m {prf,p,r,pr}, --measures {prf,p,r,pr}
#   -r {bonus,nobonus}, --regime {bonus,nobonus}
#   -t TYPES, --types TYPES
#   -o OUTFILE, --outfile OUTFILE

from argparse import ArgumentParser
from xml.dom import minidom

import codecs
import copy
import os
import re
import sys
import xml.etree.ElementTree as ET

VERBOSE = False

class Edit:
    index = ""
    start = 0
    end = 0
    part = ""
    tag = ""
    original = ""
    corrections = []

    def get_overlap(self, edit):
        """Calculates the degree of extent overlap between an edit and another.

        returns: a float between 0.00 and 1.00
        """
        if self.part == edit.part:
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
                    
                    if not self.original == '' and self.corrections == [''] and edit.corrections != ['']:
                        # gold is a deletion, the sys proposes a correction, and the gold
                        # extent is one longer than the sys
                        if  self.original[0] == ' ' and r1[1:] == r2:
                        # Gold original starts with a space and ranges are equal ignoring it
                            overlap = 1.0
                        elif self.original[-1] == ' ' and r1[:-1] == r2:
                            overlap = 1.0
                    elif not edit.original == '' and edit.corrections == [''] and self.corrections != ['']:
                        # gold is a substitution, sys is a deletion
                        if edit.original[0] == ' ' and r2[1:] == r1:
                        # sys original starts with a space and ranges are equal ignoring it
                            overlap = 1.0
                        elif edit.original[-1] == ' ' and r2[:-1] == r1:
                            overlap = 1.0

                            
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


def alignment_set(edits1, edits2, mode):
    return dict((e, get_alignments(e, edits2, mode))
                for e in edits1)


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



def has_matching_sys(gold_edit, sys_edits, case):
    """Checks whether any system edits in a list (sys_edits)
    has the same 'corrected' string as a gold edit.
    This is used for scoring correction in score_by_type().

    gold_edit -- a gold edit with >= 0 corrections
    sys_edits -- list of >= 0 sys edits with >= 0 corrections.
    case -- 'nomatch' is insensitive, 'match' is sensitive
    returns: a boolean.
    """   
    # Case 1: Gold edit proposes no corrections
    if not gold_edit.corrections:
        return True
    
    # Case 2: Iteratively try and find a physical match
    for sys_edit in sys_edits:
        # NOTE: while this iterates over list of gold corrections and sys corrections,
        # only the first sys correction is checked (in accordance with the HOO data overview p.12)
        for gold_corr in gold_edit.corrections:
            try:
                if case == 'match':
                    if gold_corr == sys_edit.corrections[0]:
                        return True
                else:
                    if gold_corr.lower() == sys_edit.corrections[0].lower():
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
    precision = float(precision)
    recall = float(recall)
    
    try:
        return 2 * ((precision * recall) / (precision + recall))
    except ZeroDivisionError:
        return 0



def process(gold_file, sys_file, outfile=None, measures='prf',
            regime='nobonus', types=None, case='nomatch', types_config=None):
    """The main function that processes the args, collects
    input, and outputs it."""
    # Parse files and extract edit objects
    gold_edits = parse_edits(gold_file)
    sys_edits = parse_edits(sys_file)
    
    # Reduce type arg into primitive types
    primitive_types = decompose_types(types, types_config)

    # Filter edits using types
    gold_edits = filter_edits_by_types(gold_edits, primitive_types)
    sys_edits = filter_edits_by_types(sys_edits, primitive_types)

    # Build a dict of the necessary information
    data = compile_results(gold_edits, sys_edits, case, measures, regime)
    
    # Insert arg type info (original, not decomposed) into dict
    if not types:
        data['types'] = 'all'
    else:
        data['types'] = types.strip('" ')

    # Insert other relevant information
    data['regime'] = regime
    data['goldfile'] = os.path.basename(gold_file.name)
    data['sysfile'] = os.path.basename(sys_file.name)

    # Output
    xml = generate_xml(data)
    print_xml(xml, outfile)
            


def decompose_types(types, types_config):
    """Decomposes the types argument into primitive types. The argument can
    either be blank, be a list of comma-separated types in quotes, or a single
    aggregated type group, defined in types_config.

    types -- the argument string of types.
    types_config -- the path to a config file if exists, or None.

    Returns: a list of primitive types.
    """
    # Note: Maybe we should check the primitive types against an
    # 'allowable types' set. Incorrect types won't cause errors though.
    # They will just do nothing.

    # Note2: argparse removes quote marks, so there isn't any
    # clean way to differentiate between primitive and aggregate
    # types. Which is why there are 'assume good as is' lines.

    
    # No types (i.e. 'all') specified
    if types == "all" or not types:
        return None
    
    if "," in types:
        # Multiple primitive types
        types = types.split(",")
    else:
        # Single primitive or aggregate type
        if types_config:
            config_data = open(types_config, 'r')
            lines = [ln.split()[1:] for ln in config_data
                     if ln.startswith("aggregate")]

            lines = [ln for ln in lines if ln[0] == types]

            if not lines:
                # Doesn't appear in config so assume it's good as is
                types = [types]
            else:
                if len(lines) > 1:
                    print '''! Multiple definitions for aggregate type '%s' found. \
Using first definition.''' % types
                # Extract the primitive types from the aggregate statement
                types = lines[0][1:]
        else:
            # Types config doesn't exist so assume it's good as is
            types = [types]

    # Take care of overlaps where 'all' is specified with other types
    if "all" not in types:
        print "Primitive types: %s" % types
        return types

    return None



def filter_edits_by_types(edits, types=None):
    """Filters a set of edits to only those that have types as
    specified in the 'types' parameter. If 'types' is None, no
    filtering is performed. Filtering is only performed if each
    and every edit in the set has a valid type (tag attribute).
    """
    # 1. No filter (no types specified)
    if not types:
        return edits

    # 2. No filter (every edit does not have a valid tag)
    for e in edits:
        if e.tag == None:
            return edits

    # 3. Filter
    edits = [e for e in edits if e.tag in types]
    
    return edits
            

# TODO: no default case!!
def compile_results(gold_edits, sys_edits, case,
                    measures='prf', regime='nobonus'):
    """Assembles all the information corresponding to the children
    of the <results> element in the results file.
    """
    counts = compile_counts(gold_edits, sys_edits, case)
    scores = compile_scores(counts, measures, regime)
    goldedits = compile_gold_edits(gold_edits, sys_edits, case)
    spuriousedits = compile_spurious_edits(gold_edits, sys_edits)

    return dict(counts=counts, scores=scores,
                goldedits=goldedits, spuriousedits=spuriousedits)


    
def compile_counts(gold_edits, sys_edits, case):
    """Combines counts of specified types for use in evaluation.

    gold_edits -- a list of edit objects taken from the gold edit set.
    sys_edits -- a list of edit objects taken from the sys edit set.

    Returns a dict of counts:
      (total, detections, recognitions, corrections, missed_optional,
      spurious, system)
    """
    if VERBOSE: print "Compile counts..."
    
    # Generate overall alignment sets.
    lenient_aligns = make_alignment_sets(gold_edits, sys_edits, 'lenient')
    strict_aligns = make_alignment_sets(gold_edits, sys_edits, 'strict')

    # Number of gold edits
    gold = len(lenient_aligns['gold'])

    # Number of gold edits with at least one leniently aligned system edit
    detected = sum(1 for v in lenient_aligns['gold'].itervalues() if v)

    # Number of gold edits with at least one strictly aligned system edits
    recognised = sum(1 for v in strict_aligns['gold'].itervalues() if v)

    # Number of recognised gold edits that are string matched by aligned edits
    corrected = sum(1 for k,v in strict_aligns['gold'].iteritems()
                      if v and has_matching_sys(k, v, case))

    # Number of optional gold edits that weren't detected
    missed_optional = sum(1 for k,v in lenient_aligns['gold'].iteritems()
                               if k.is_optional() and not v)

    # Number of system edits that don't align to any gold edits.
    spurious = sum(1 for v in lenient_aligns['sys'].itervalues() if not v)

    # Number of system edits. 
    system = len(lenient_aligns['sys'])

    return dict(gold=gold, detected=detected, recognised=recognised,
                corrected=corrected, missed_optional=missed_optional,
                spurious=spurious, system=system)               



def compile_scores(counts, measures='prf', regime='nobonus'):
    if VERBOSE: print "Compiling scores..."
    
    # Make missed optionals count for nothing under nobonus regime.
    # We make a deep copy of counts because we don't want to modify
    # it directly as it is used elsewhere.
    if regime == 'nobonus':
        counts = copy.deepcopy(counts)
        counts['missed_optional'] = 0

    detection = calculate_detection_scores(counts, measures)
    recognition = calculate_recognition_scores(counts, measures)
    correction = calculate_correction_scores(counts, measures)

    return dict(detection=detection,
                recognition=recognition,
                correction=correction)


def calculate_detection_scores(counts, measures='prf'):
    gold = counts['gold']
    detected = counts['detected']
    missed_optional = counts['missed_optional']
    spurious = counts['spurious']
    
    try:
        precision = float(detected + missed_optional) / \
                    (detected + spurious + missed_optional)
    except ZeroDivisionError:
        precision = 1

    try:
        recall = float(detected + missed_optional) / gold
    except ZeroDivisionError:
        recall = 1

    fscore = calculate_Fscore(precision, recall)

    return filter_measures(precision, recall, fscore, measures)

    

def calculate_recognition_scores(counts, measures='prf'):
    gold = counts['gold']
    system = counts['system']
    recognised = counts['recognised']
    missed_optional = counts['missed_optional']
    
    try:
        precision = float(recognised + missed_optional) / \
                    (system + missed_optional)
    except ZeroDivisionError:
        precision = 1

    try:
        recall = float(recognised + missed_optional) / gold
    except ZeroDivisionError:
        recall = 1

    fscore = calculate_Fscore(precision, recall)

    return filter_measures(precision, recall, fscore, measures)



def calculate_correction_scores(counts, measures='prf'):
    gold = counts['gold']
    system = counts['system']
    corrected = counts['corrected']
    missed_optional = counts['missed_optional']
    
    try:
        precision = float(corrected + missed_optional) / \
                    (system + missed_optional)
    except ZeroDivisionError:
        precision = 1
        
    try:
        recall = float(corrected + missed_optional) / gold
    except ZeroDivisionError:
        recall = 1

    fscore = calculate_Fscore(precision, recall)

    return filter_measures(precision, recall, fscore, measures)



def filter_measures(precision, recall, fscore, measures='prf'):
    """Compiles a dictionary of only the specified measures."""
    scores = dict()

    if 'p' in measures:
        scores['precision'] = precision

    if 'r' in measures:
        scores['recall'] = recall

    if 'f' in measures:
        scores['fscore'] = fscore

    return scores


    
def compile_gold_edits(gold_edits, sys_edits, case):
    """Compiles a list of gold edits, reporting their type, optionality,
    and whether they missed, detected, recognised, or corrected.

    Returns a list of dicts (index, type, optional, status).
    """
    if VERBOSE: print "Compiling gold edits..."
    edit_list = list()

    # Filter to specified types
    #if types:
    #gold_edits = [e for e in gold_edits if e.tag in types]
        
    for g in gold_edits:
        gdict = dict(index=g.index, type=g.tag, optional=str(g.is_optional()))

        status = "Missed"

        # System edits that have a strict extent match with the gold edit
        strict_aligned_sys = get_alignments(g, sys_edits, mode='strict')

        if strict_aligned_sys:
            # Correct if it has a match in the system
            if has_matching_sys(g, strict_aligned_sys, case):
                status = "Corrected"
            else:
                status = "Recognised"
        else:
            # Any lenient overlap means 'detected'
            for s in sys_edits:
                if g.get_overlap(s):
                    status = "Detected"
                    break

        gdict['status'] = status
        edit_list.append(gdict)

    # Sort by index
    edit_list.sort(key=lambda x: x['index'])

    return edit_list


def compile_spurious_edits(gold_edits, sys_edits):
    """Compile a list of spurious edits, reporting their index,
    type (if present), and position. Spurious edits are those
    that don't align with any gold edits.

    Returns a list of dicts (index, type, start, end).
    """
    if VERBOSE: print "Compiling spurious edits..."

    # Compile raw spurious edits
    spurious_edits = [s for s in sys_edits
                      if not get_alignments(s, gold_edits, 'lenient')]

    # Transform into a list of dicts
    results = [dict(index=s.index, type=s.tag, start=str(s.start), end=str(s.end))
               for s in spurious_edits]

    # Sort by index
    results.sort(key=lambda x: x['index'])

    return results



def parse_edits(edits_file):
    """Parses the XML edits into a list of Edit objects. We
    use objects instead of dictionaries so that they can be used
    as hash keys in gold->[system] mappings later on. Also make
    sure that <empty/> objects are turned into empty strings.

    returns: a list of Edit objects.
    """
    edits = list()
    root = ET.parse(edits_file)

    if VERBOSE: print "Parsing edits in %s..." % os.path.basename(edits_file.name)

    for e in root.getiterator("edit"):
        edit = Edit()
        edit.index = e.get('index')
        if edit.index == None:
            edit.index = "-1"
        edit.start = int(e.get('start'))
        edit.end = int(e.get('end'))
        edit.part = e.get('part')
        edit.tag = e.get('type', default='')

        # Replace <empty/> in original with ""
        original_elem = e.find('original')

        if original_elem.find("empty") is None:
            edit.original = original_elem.text
        else:
            edit.original = ""


        # Compile corrections and replace <empty/> tags with ""
        corrections_elem = e.find('corrections')

        if corrections_elem is not None:
            corrections = corrections_elem.findall("correction")
            has_empty = bool(sum(c.find("empty") is not None for c in corrections))

            if has_empty:
                edit.corrections = [c for c in corrections
                                    if c.find("empty") is None]
                edit.corrections.append("")
            else:
                edit.corrections = [c.text for c in corrections]
        
        edits.append(edit)

    if VERBOSE: print "Found %d edits" % len(edits)
            
    return edits



def generate_xml(data):
    """Generate an XML ElementTree model corresponding to
    the information contained in data.

    data -- a complex dict of dicts and lists.

    Returns an ElementTree object.
    """
    if VERBOSE: print "Generating XML..."
    
    # Top element
    results = ET.Element('results')
    results.set("gold", data['goldfile'])
    results.set("system", data['sysfile'])

    # Types
    types = ET.SubElement(results, 'types')
    types.text = data['types']

    # Regime
    regime = ET.SubElement(results, 'regime')
    regime.text = data['regime']

    # Counts
    counts = ET.SubElement(results, 'counts')

    for k,v in data['counts'].iteritems():
        element = ET.SubElement(counts, k)
        element.text = str(v)

    # Scores
    scores = ET.SubElement(results, 'scores')

    for t in data['scores']:
        score_type = ET.SubElement(scores, t)

        for m,v in data['scores'][t].iteritems():
            measure = ET.SubElement(score_type, m)
            measure.text = str(v)

    # Gold edits
    goldedits = ET.SubElement(results, 'goldedits')

    for edit in data['goldedits']:
        element = ET.SubElement(goldedits, 'edit', edit)

    # Spurious edits
    spuriousedits = ET.SubElement(results, 'spuriousedits')

    for edit in data['spuriousedits']:
        element = ET.SubElement(spuriousedits, 'edit', edit)

    return results



def print_xml(elem, outfile=None):
    """Prints out the edit information to file or sysout."""
    if outfile:
        f = codecs.open(outfile, 'w', 'utf_8')
    else:
        f = sys.stdout

    f.write(prettify(elem))
    f.close()




def prettify(elem):
    """Reparse ET to minidom to enable pretty printing.
    Adjusts using regex so text isn't on an individual line.
    """
    rough_string = ET.tostring(elem, 'utf-8')
    reparsed = minidom.parseString(rough_string)
    uglyXml = reparsed.toprettyxml(indent='  ')

    text_re = re.compile('>\n\s+([^<>\s].*?)\n\s+</', re.DOTALL)    
    prettyXml = text_re.sub('>\g<1></', uglyXml)

    return prettyXml
    


if __name__ == '__main__':
    VERBOSE = True
    
    # Set up the command line utility
    parser = ArgumentParser(description="Compare two files containing edit structures.")
    parser.add_argument("-m", "--measures", default="prf", dest="measures", \
                        choices=['prf','p','r','pr'])
    
    parser.add_argument("-r", "--regime", default="nobonus", dest="regime", \
                        choices=['bonus','nobonus'])

    parser.add_argument("-t", "--types", default="all", dest="types")
    parser.add_argument("-c", "--case", default="nomatch", dest="case", \
                        choices=["match", "nomatch"])
    parser.add_argument("goldedits", type=file)
    parser.add_argument("sysedits", type=file)
    parser.add_argument("-o", "--outfile", default=None, dest="outfile")

    # Get the arguments and execute
    args = parser.parse_args()

    # Find an types.config file if available
    current_dir = os.getcwd()
    config_file = os.path.join(current_dir, "types.config")

    if not os.path.exists(config_file):
        config_file = None
    
    process(args.goldedits, args.sysedits, args.outfile, args.measures,
            args.regime, args.types, args.case, config_file)
