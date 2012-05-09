#!/usr/bin/python
# -*- coding: utf-8 -*-

# extractedits.py
#
# Author:	George Narroway 
# Date:		26 June 2011
# Version:      2.1
#
# Copyright (c) Centre for Language Technology, Macquarie University
# Not to be used for commercial purposes.
#
# HOO Task:
# Given a gold-standard file in XML format, extract each edit into a standoff
# XML representation, appending character offsets.
# 
# Usage:
# python extractedits.py <input.xml> ..................... print to standard out
# python extractedits.py <input.xml> [-o <outputfile>] ... write to file


from optparse import OptionParser
import re
import sys
import xml.etree.cElementTree as ET
from xml.dom import minidom
import codecs


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
        text = open(args[0]).read()
        text = strip_tags(text)
        edits = get_edits(text)
        edit_tree = create_element_tree(edits)
    
        print_xml(edit_tree, options.output)
    except:
        print_usage()


def create_element_tree(edits):
    """Add a list of edits to a root node."""
    top = ET.Element('edits')
    edits.sort(key = lambda x: x.get('index'))

    for edit in edits:
        top.append(edit)

    return top    


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

"""d[713:718]"""

def get_edits(text):
    """Extract edit annotations and all their information while keeping track
    of character offsets relative to the original text sans edits.
    Returns: edit information as a list of element tree (XML) elements.
    """
    edit_p  = re.compile("(?P<open><edit.*?>)(?P<inner>.*?)(?P<close></edit>)")
    corr_p = re.compile("<corrections>.*?</corrections>")
    edits = []

    offset = 0

    for m in re.finditer(edit_p, text):
        # Make an edit object
        edit_text = "".join(m.groups())
        edit = ET.XML(m.group(0))

        # Set the bounds of the original text and adjust offset
        inner_string = m.group('inner')   
        start = m.start() - offset
        corr_m = re.search(corr_p, inner_string)
        
        if corr_m: # Replacement/insertion have a correction
            offset += len(corr_m.group(0))           
            
            if not inner_string.startswith("<empty/>"):
                end = start + corr_m.start()
            else:
                offset += len("<empty/>") # It is "" in plain text
                end = start
        else:
            # Deletions may not have a correction
            if not inner_string.startswith("<empty/>"):
                end = start + len(inner_string)
            else: # Unspecified error <empty/> is "" in plain text
                end = start
                offset += len(inner_string)


        edit.set("start", "%d" % start)        
        edit.set("end", "%d" % end)

        offset += len(m.group('open')) + len(m.group('close'))
            

        # Make the original text a subelement of <edit>
        # Original text may be a string or <empty/> element.
        original = ET.SubElement(edit, "original")
        
        if edit.text:
            original.text = edit.text
            edit.text = ""
        else:
            empty = edit.find('empty')
            
            try:
                edit.remove(empty)
                original.append(empty)
            except Exception as e:
                pass
                 
        edits.append(edit)

    return edits
    


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
# Given a gold-standard file in XML format, extract each edit into a standoff
# XML representation, appending character offsets.
# 
# Usage:
# python extractedits.py <input.xml> ..................... print to standard out
# python extractedits.py <input.xml> [-o <outputfile>] ... write to file
"""
        

if __name__ == "__main__":
    try:
        main()
    except IOError, e:
        print_err(e)
        print_usage()
        sys.exit(1)

# end of program
