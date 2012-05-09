# File:     evalrun.py
#
# Author:	George Narroway 
# Modified:	24 April 2012
# Version:      0.6
#
# Copyright (c) Centre for Language Technology, Macquarie University
# Not to be used for commercial purposes.
#
# 
# Changes in Version 0.6
# This version adds type and case arguments to make it compatible with the 
# evalfrag.py version 0.7 and above
#
# Purpose:
# Compares two directories containing corresponding edit sets.
#
# positional arguments:
#   goldeditdir
#   syseditdir
#
# optional arguments:
#   -h, --help            show this help message and exit
#   -m {prf,p,r,pr}, --measures {prf,p,r,pr}
#   -r {bonus,nobonus}, --regime {bonus,nobonus}
#   -t TYPES, --types TYPES
#   -c {match,nomatch}, --case {match,nomatch}
#   -o OUTFILE, --outfile OUTFILE
  
from argparse import ArgumentParser, ArgumentTypeError
from datetime import datetime
from xml.dom import minidom

import codecs
import os
import re
import sys
import xml.etree.cElementTree as ET

import evalfrag
reload(evalfrag)


def process(golddir, sysdir, outfile=None, measures='prf',
            regime='nobonus', types=None, case='nomatch',types_config=None):
    """The main function that processes the args, collects
    input, and outputs it."""
    # Find (gold,sys) pairs of edit files
    try:
        pairs = match_files(golddir, sysdir)
    except Exception as e:
        print e
        sys.exit()

    # Generate counts and scores
    print "Compiling scores..."
    primitive_types = evalfrag.decompose_types(types, types_config)
    
    counts = compile_counts(pairs, primitive_types, case)
    scores = evalfrag.compile_scores(counts, measures, regime)

    # Create dict of results
    data = dict(goldpath=golddir, syspath=sysdir,
                counts=counts, scores=scores, regime=regime)

    # Insert arg types (not primitive) into dict
    if not types:
        data['types'] = 'all'
    else:
        data['types'] = types.strip('" ')

    # Insert time stamp into dict
    dt = datetime.now()
    timestamp = dt.strftime("%a %d %b %Y %I:%M%p")
    data['timestamp'] = timestamp


    # Insert team and run info into dict
    sys_file = compile_files(sysdir)[0]
    name = sys_file.split('.')[0]
    data['run'] = name[-1]
    data['team'] = name[-3:-1]

    # Output
    xml = generate_xml(data)
    print_xml(xml, outfile)
    

def compile_files(root):
    """Generates a list of full paths corresponding 
    to the files in a given source directory.
    """
    files = [os.path.join(root, f) for f in os.listdir(root)
             if not f.startswith(".")]
    
    return files


def match_files(gold_folder, sys_folder):
    """Find pairs of corresponding files.
    Gold files are of the form '0001GE.xml'.
    Corresponding sys files must start with '0001'."""

    print "Compiling files..."
    # Get a list of files in the folders supplied.
    gold_files = compile_files(gold_folder) # nnnnGE.xml
    sys_files = compile_files(sys_folder)   # nnnnXXN.xml

    if len(gold_files) != len(sys_files):
        raise Exception("Number of system edit files (%d) must\
equal number of gold edit files (%d)." % (len(sys_files), len(gold_files)))

    print "%d gold files found in %s" % (len(gold_files), basename(gold_folder))
    print "%d system files found in %s\n" % (len(sys_files), basename(sys_folder))

    print "Matching system files to gold files..."
    
    # Match them up, where nnnn must be common in a pair.
    pairs = [(f1, f2) for f1 in gold_files for f2 in sys_files
             if basename(f2).startswith(basename(f1).split("GE.")[0])]
    
    if len(pairs) != len(gold_files):
        raise Exception("Each gold edit file must have a single \
                        corresponding system edit file.")
    
    return pairs


def basename(path):
    """Convenience function."""
    return os.path.basename(path)


def compile_counts(pairs, types, case):
    counts = dict()
    
    for g,s in pairs:
        print g, s
        gold_edits = evalfrag.filter_edits_by_types(evalfrag.parse_edits(g), types)
        sys_edits = evalfrag.filter_edits_by_types(evalfrag.parse_edits(s), types)
        pair_counts = evalfrag.compile_counts(gold_edits, sys_edits, case)

        for k,v in pair_counts.iteritems():
            counts[k] = counts.get(k, 0) + v

    return counts


def generate_xml(data):
    """Generate an XML ElementTree model corresponding to
    the information contained in data.

    data -- a complex dict of dicts and lists.

    Returns an ElementTree object.
    """
    print "Generating XML..."
    # Top element
    results = ET.Element('results')
    results.set('timestamp', data['timestamp'])
    results.set('sysid', data['team'])
    results.set('runid', data['run'])

    # Paths
    goldpath = ET.SubElement(results, "goldpath")
    goldpath.text = data['goldpath']
    syspath = ET.SubElement(results, "syspath")
    syspath.text = data['syspath']
    
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


def directory(s):
    if not os.path.isdir(s):
        msg = "%r is not a directory" % s
        raise ArgumentTypeError(msg)
    
    return s


if __name__ == '__main__':
    # Set up the command line utility
    parser = ArgumentParser(description="Compares two directories containing corresponding edit sets.")
    parser.add_argument("-m", "--measures", default="prf", dest="measures", \
                        choices=['prf','p','r','pr'])
    
    parser.add_argument("-r", "--regime", default="nobonus", dest="regime", \
                        choices=['bonus','nobonus'])

    parser.add_argument("-t", "--types", default="all", dest="types")
    parser.add_argument("-c", "--case", default="nomatch", dest="case", \
                        choices=["match", "nomatch"])
    parser.add_argument("goldeditdir", type=directory)
    parser.add_argument("syseditdir", type=directory)
    parser.add_argument("-o", "--outfile", default=None, dest="outfile")

    # Get the arguments and execute
    args = parser.parse_args()

    # Find an types.config file if available
    current_dir = os.getcwd()
    config_file = os.path.join(current_dir, "types.config")

    if not os.path.exists(config_file):
        config_file = None
    
    process(args.goldeditdir, args.syseditdir, args.outfile, args.measures,
            args.regime, args.types, args.case, config_file)
