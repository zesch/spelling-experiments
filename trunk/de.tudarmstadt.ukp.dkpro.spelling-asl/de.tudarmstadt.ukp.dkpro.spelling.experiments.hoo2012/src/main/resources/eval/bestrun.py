# File:     bestrun.py
#
# Author:	George Narroway 
# Modified:	17 December 2011
# Version:      0.1
#
# Copyright (c) Centre for Language Technology, Macquarie University
# Not to be used for commercial purposes.
#
# Purpose:
# Identifies the best run from a series of runs based on certain parameters.
#
# usage: bestrun.py [-h] [-m {p,precision,r,recall,f,fscore}]
#                   [-s {detection,recognition,correction}]
#                   inputdir outputdir
# 
# positional arguments:
#   inputdir
#   outputdir
# 
# optional arguments:
#   -h, --help            show this help message and exit
#   -m {p,precision,r,recall,f,fscore}, --measure {p,precision,r,recall,f,fscore}
#   -s {detection,recognition,correction}, --score {detection,recognition,correction}

from argparse import ArgumentParser, ArgumentTypeError

import os
import shutil
import xml.etree.cElementTree as ET

MEASURES = ['precision', 'recall', 'fscore']


def process(inputdir, outputdir, measure='f', scoretype='detection'):
    """Processes a directory of results files (inputdir) and
    copies the best to outputdir, with 'best' determined by the
    combination of measure and scoretype. The extension of the file
    is modified to the scoretype."""
    files = compile_files(inputdir)

    data = [parse(f, measure, scoretype) for f in files]

    # Take the file from max of the list, as determined by the score
    best_file = max(data, key=lambda x: x[1])[0]

    copy_to_dir(best_file, outputdir, scoretype)
    
    

def compile_files(root):
    """Generates a list of full paths corresponding 
    to the files in a given source directory.
    """
    files = [os.path.join(root, f) for f in os.listdir(root)
             if not f.startswith(".")]
    
    return [f for f in files if os.path.isfile(f)]



def parse(f, measure='f', scoretype='detection'):
    """Parses a results file (f) and returns a (f, score) pair
    where the score is the value of the scoretype/measure combination.
    Such a pair can then be analysed to return the file with the best score.
    """
    root = ET.parse(f)
    output = dict()

    # Expand measures args into element names
    measurenames = list()

    if measure == 'p':
        measure = 'precision'
    elif measure == 'r':
        measure = 'recall'
    elif measure == 'f':
        measure = 'fscore'
        
    # Find the relevant scoretype/measure combination
    score = root.find("scores/%s/%s" % (scoretype, measure)).text

    return (f, score)



def copy_to_dir(f, outputdir, extension='detection'):
    """Copies a file (f) to outputdir, modifying its extension,
    typically to the scoretype passed as an argument to bestrun.
    """
    basename = os.path.basename(f)
    outfile = "%s.%s" % (basename.split('.')[-2], extension)
    outpath = os.path.join(outputdir, outfile)

    if not os.path.exists(outputdir):
        os.path.makedirs(outputdir)

    shutil.copy(f, outpath)

    

def directory(s):
    if not os.path.isdir(s):
        msg = "%r is not a directory" % s
        raise ArgumentTypeError(msg)
    
    return s



if __name__ == '__main__':    
    # Set up the command line utility
    parser = ArgumentParser(description="Identifies the best run from a series of runs based on certain parameters.")
    parser.add_argument("-m", "--measure", default="f", dest="measure", \
                        choices=['p','precision','r','recall', 'f', 'fscore'])
    parser.add_argument("-s", "--score", default='detection', dest="scoretype", \
                        choices=['detection', 'recognition', 'correction'])

    parser.add_argument("inputdir", type=directory)
    parser.add_argument("outputdir", type=directory)


    # Get the arguments and execute
    args = parser.parse_args()
    
    process(args.inputdir, args.outputdir, args.measure, args.scoretype)

        



