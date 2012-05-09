#!/usr/bin/python
# -*- coding: utf-8 -*-

# diffextract.py
#
# Author:	George Narroway 
# Date:		24 July 2011
# Version:      2.1
#
# Copyright (c) Centre for Language Technology, Macquarie University
# Not to be used for commercial purposes.
#
# HOO Task:
# Given a wdiff file (obtained by "wdiff original.txt new.txt > output.txt"),
# extracts the differences into a standoff XML format.
# 
# Usage:
# python diffextract.py <input.diff> ......................... print to standard out
# python diffextract.py <input.diff> [-o <outputfile.xml>] ... write to file

from optparse import OptionParser
import re
import sys
import xml.etree.cElementTree as ET
from xml.dom import minidom
import codecs
import os

PUNCTUATION = list(r"""'"[({}]).,;:!?""")

def main():
    # OptionParser usage:
    # extractedits.py in.diff -o out.xml -->
    # args[0] = 'in.diff'
    # options: {'output':'out.xml']
    parser = OptionParser(__doc__)
    parser.add_option("-o", "--output", dest="output", default=None,
                      help="specify a file name to write to")
    (options, args) = parser.parse_args()

    try:
        input_file = args[0]
        xml = process_file(input_file)
        print_xml(xml, options.output)
    except:
        print_usage()



def process_file(input_file):
    """Runs the edit extraction process on a wdiff output file.
    input_file -- output from 'wdiff original.txt new.txt > output.diff'

    returns: an XML representation (ElementTree) of the extracted edits.
    """
    text = open(input_file).read()
    text = strip_tags(text)
    text = externalise_punctuation(text)
    edits = get_edits(text)     # Pull the edits from the wdiff file. using regex
    edits = consolidate(edits)  # Make edit pairs from single edits.
    file_name = os.path.basename(input_file).split(".")[0]

    return create_elements(edits, file_name) # XML representation



def process_string(text, file_name):
    """Runs the extraction process using wdiff output in memory, to
    allow calling from external modules without creating wdiff files.
    text -- output from wdiff.
    file_name -- an identifier passed into the xml.

    returns: an XML representation (ElementTree) of the extracted edits.
    """
    
    text = strip_tags(text)
    text = externalise_punctuation(text)
    edits = get_edits(text)     # Pull the edits from the wdiff file. using regex
    edits = consolidate(edits)  # Make edit pairs from single edits.

    return create_elements(edits, file_name) # XML representation



def strip_tags(text):
    """Strips text of non-edit tags, resulting in text that is
    the original plain text with edit annotations only.
    """
    # Remove header tags
    p = re.compile("<\?.+?\?>") 
    text = re.sub(p, "", text)

    # Remove <HOO>, <p> and <s> tags
    text = text.replace("<p>","")
    text = text.replace("</p>","")
    text = text.replace("<s>","")
    text = text.replace("</s>","")
    text = text.replace("<HOO>","")
    text = text.replace("</HOO>","")

    return text



def externalise_punctuation(text):
    """Changes diff input from '[-foo.-] {+foo;+}' to 'foo[-.-] {+;+}'
    as the preceding word should not be part of the edit.
    """
    # Replacement right: [-foo.-] {+foo;+}' => 'foo[-.-] {+;+}'
    replace_right = """\[-(?P<txt>[\S]+?)(?P<del_punc>['"\]),.:;!?])-\](\s*?){\+(?P=txt)(?P<ins_punc>['"\]),.:;!?])\+}"""
    text = re.sub(replace_right, '\g<txt>[-\g<del_punc>-] {+\g<ins_punc>+}', text)

    # Replacement left: [-[foo.-] {+(foo+}' => '[-[-]{+(+}foo'
    replace_left = """\[-(?P<del_punc>['"\[({])(?P<txt>[\S]+?)-\](\s*?){\+(?P<ins_punc>['"\[({])(?P=txt)\+}"""
    text = re.sub(replace_left, '[-\g<del_punc>-] {+\g<ins_punc>+}\g<txt>', text)

    # Insertion right: [-foo-] {+foo;+}' => 'foo{+;+}'
    insert_right = """\[-(?P<txt>[\S]+?)-\](\s*?){\+(?P=txt)(?P<ins_punc>['"\]),.:;!?])\+}"""
    text = re.sub(insert_right, '\g<txt>{+\g<ins_punc>+} ', text)

    # Insertion left: [-foo-] {+"foo+}' => '{+"+}foo'
    insert_left = """\[-(?P<txt>[\S]+?)-\](\s*?){\+(?P<ins_punc>['"\[({])(?P=txt)\+}"""
    text = re.sub(insert_left, '{+\g<ins_punc>+}\g<txt> ', text)

    # Delete right: [-foo.-] {+foo+}' => 'foo[-.-]'
    delete_right = """\[-(?P<txt>[\S]+?)(?P<del_punc>['"\]),.:;!?])-\](\s*?){\+(?P=txt)\+}"""
    text = re.sub(delete_right, '\g<txt>[-\g<del_punc>-]', text)

    # Delete left: [-.foo-] {+foo+}' => '[-.-]foo'
    delete_left = """\[-(?P<del_punc>['"\[({])(?P<txt>[\S]+?)-\](\s*?){\+(?P=txt)\+}"""
    text = re.sub(delete_left, '[-\g<del_punc>-]\g<txt>', text)

    # If edit is at start of line, wdiff outputs [-old-]\n\n{+new+} => [-old] {+new+}
    # Reduces extra spaces introduced by a system between [-old-] and {+new+} to a single space.
    error_re = """(\[-.+?-\])[\s]+(\{\+.+?\+\})"""
    text = re.sub(error_re, '\\1 \\2', text)

    #print text
  
    return text



def get_edits(data):
    """Extract both insertions and deletions into a single list of edits.
    We keep track of text offsets for both the old and new source files.
    4: Remove just the edit noise '[--]' because the text exists in the file.
    m.end-m.start+1: The whole edit string '[-edit-]' and an additional space.
    """
    insertion_reg = "{\+(?P<ins>.+?)\+}"
    deletion_reg = "\[-(?P<del>.+?)-\]"
    edit_reg = "%s|%s" % (insertion_reg, deletion_reg)
    edits = []

    new_offset = 0
    old_offset = 0

    # Each match can only be _either_ a deletion or an insertion.
    # Start and end are the indexes for the entire edit string.
    for m in re.finditer(edit_reg, data):
        if m.group('del'):                
            edits.append({'type': "old", 'text': m.group('del'),
                          'start': m.start(), 'end': m.end(),
                          'offset_new': new_offset, 'offset_old': old_offset})

            # edit at start of file does not have a preceding space therefore smaller offset
            if m.start() == 0:
                old_offset += 3
                new_offset += m.end() - m.start()
            else:
                old_offset += 4
                new_offset += m.end() - m.start() + 1
        else:
            edits.append({'type': "new", 'text': m.group('ins'),
                          'start': m.start(), 'end': m.end(),
                          'offset_new': new_offset, 'offset_old': old_offset})

            old_offset += m.end() - m.start() + 1
            new_offset += 4

    return edits
    


def consolidate(edits):
    """Combines adjacent edit pairs."""
    consolidated_edits = []
    
    used = False

    for i,e in enumerate(edits):
        if i < len(edits) - 1:
            if not used:
                edit = create_dict(e)
                
                if adjacent(e, edits[i+1]):
                    # A pair of edits.
                    edit.update(create_dict(edits[i+1])) # Combone paired info.
                    consolidated_edits.append(edit)
                    used = True
                else:
                    # A single edit.
                    edit = set_defaults(edit) # Create DELETE/INSERT info.
                    consolidated_edits.append(edit)
            else:
                # This edit was already combined (and used).
                used = False
        else:
            if not used:
                edit = create_dict(e)
                # A single edit in the last position.
                edit = set_defaults(edit) # Create DELETE/INSERT info.
                consolidated_edits.append(edit)

    return consolidated_edits



def adjacent(edit1, edit2):
    """Tests whether the next edit string starts up to three spaces after
    the current edit string. One-way test as the list of edits is
    already sorted so it can't be adjacent the other way.
    """
    if edit2['start'] - edit1['end'] <= 1:
        return True

    return False



def create_dict(e):
    """Creates an edit dictionary with the type as key.
    input: {type, text, start, end, offset_new, offset_old}
    output: {type: {text, start, end, offset_new, offset_old}}
    """
    t = e['type']
    e.pop('type')
    return {t: e}    



def set_defaults(e):
    """For edits without an adjacent pair, set the corresponding
    edit to INSERT or DELETE. The '4' is the length of the edit
    noise ("{--}") and is removed in create_elements().
    """
    if 'old' not in e:
        e['old'] = dict(text="INSERT", offset_old=e['new']['offset_old'],
                        start=e['new']['start'], end=e['new']['start']+4)
        # Insert a trailing space
        if e['new']['text'] not in PUNCTUATION:
            e['new']['text'] = "%s " % e['new']['text']
            e['new']['end'] += 1
    elif 'new' not in e:
        e['new'] = dict(text="DELETE", offset_new=e['old']['offset_new'],
                        start=e['old']['start'], end=e['old']['start']+4)
        # Also remove a trailing space.
        if e['old']['text'] not in PUNCTUATION:
            e['old']['text'] = "%s " % e['old']['text']
            e['old']['end'] += 1

    return e

    

def create_elements(edits, file_name):
    top = ET.Element('edits')
    
    for i,e in enumerate(edits):
        old_span = dict(start="%d" % (e['old']['start'] - e['old']['offset_old']),
                        end="%d" % (e['old']['end'] - e['old']['offset_old'] - 4), # 4 adjusts for [--]
                        index="%s-%04d" % (file_name, (i+1))) 

        child = ET.SubElement(top, 'edit', old_span)
        old = ET.SubElement(child, 'original')

        # Change an insertion original to <empty/> element
        if e['old']['text'] == "INSERT":
            old_empty = ET.SubElement(old, 'empty')
        else:
            old.text = e['old']['text'].decode('latin-1')
            
        # <correction> must be in a <corrections> element
        corrections = ET.SubElement(child, 'corrections')
        new = ET.SubElement(corrections, 'correction')

        # Change a deletion correction into an <empty/> correction
        if e['new']['text'] == "DELETE":
            new_empty = ET.SubElement(new, 'empty')
        else:
            new.text = e['new']['text'].decode('latin-1')

    return top
                            

def prettify(elem):
    """Reparse ET to minidom to enable pretty printing.
    Adjusts using regex so text isn't on an individual line."""
    rough_string = ET.tostring(elem, 'utf-8')
    reparsed = minidom.parseString(rough_string)
    uglyXml = reparsed.toprettyxml(indent='  ')

    text_re = re.compile('>\n\s+([^<>\s].*?)\n\s+</', re.DOTALL)    
    prettyXml = text_re.sub('>\g<1></', uglyXml)

    return prettyXml
    


def print_xml(elem, outfile=None):
    """Prints out the edit information to file or sysout."""
    if outfile:
        f = codecs.open(outfile, 'w', 'utf_8')
    else:
        f = sys.stdout

    f.write(prettify(elem))
    f.close()



def print_usage():
    print """# HOO Task:
# Given a wdiff file (obtained by "wdiff original.txt new.txt > output.txt"),
# extracts the differences into a standoff XML format.
# 
# Usage:
# python diffextract.py <input.diff> ......................... print to standard out
# python diffextract.py <input.diff> [-o <outputfile.xml>] ... write to file
"""


if __name__ == "__main__":
    try:
        main()
    except IOError, e:
        print_usage()
        sys.exit(1)

# end of program
