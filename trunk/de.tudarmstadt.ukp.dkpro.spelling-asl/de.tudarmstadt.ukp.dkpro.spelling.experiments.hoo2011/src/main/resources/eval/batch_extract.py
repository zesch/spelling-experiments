# batch_extract.py
#
# Author:	George Narroway 
# Date:		24 June 2011
# Version       2.1
#
# Copyright (c) Centre for Language Technology, Macquarie University
# Not to be used for commercial purposes.
#
# Processes two folders contaiing original and system-generated files
# respectively. Generates wdiff output for each pair of files,
# then runs extractedits.py to generate an xml structure for the
# differences.
#
# Usage: python batch_extract.py <original> <system> <output>
# original -- folder of original plain text (0001.txt).
# system --- folder of system-generated edited plain text (0001XXN.txt).
# output --- an output folder, which will contain 0001XXN.xml.
#
# XX: Team ID.
# N: Run number.
# It is possible to have several 'runs' for the same source (original) file
# processed in one go. ie. 0001.txt can be compared with 0001AB1.txt and 0001AB2.txt.
#

import sys
import re
import diffextract
import os
import subprocess
reload(diffextract)


def compile_files(root):
    """Generates a list of full paths corresponding 
    to the files in a given source directory.
    """
    files = [os.path.join(root, f) for f in os.listdir(root) if not f.startswith(".")]
    
    return files


def base_name(path):
    return os.path.basename(path)


def match_files(original_folder, sys_folder):
    print "Compiling files..."
    orig_files = compile_files(original_folder) # nnnn.txt
    sys_files = compile_files(sys_folder) # nnnnXXN.txt

    print "%d original files found in %s" % (len(orig_files), base_name(original_folder))
    print "%d system files found in %s\n" % (len(sys_files), base_name(sys_folder))

    print "Matching system files to original files..."
    pairs = [(s1, s2) for s1 in orig_files for s2 in sys_files
             if base_name(s2).startswith(base_name(s1).split(".")[0])]

    return pairs


def extract_diffs(pairs, output_folder):
    print "Extract diffs..."
    
    for (p1,p2) in pairs:
        args = ['wdiff', "%s" % p1, "%s" % p2]
        p = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        wdiff, errors = p.communicate()

        out_file = os.path.basename(p2).split(".")[0]         # 0001AB1
        out_path = os.path.join(output_folder, "%s.xml" % out_file)  # ../0001AB1.xml
        xml = diffextract.process_string(wdiff, out_file)
        diffextract.print_xml(xml, out_path)
        

if __name__ == '__main__':
    if len(sys.argv) == 4:
        original_folder = sys.argv[1]
        sys_folder = sys.argv[2]
        output_folder = sys.argv[3]
        try:
            pairs = match_files(original_folder, sys_folder)

            if pairs:
                print "%d pairs found" % len(pairs)
                extract_diffs(pairs, output_folder)
            else:
                raise Exception("no pairs")
        except:
            print """An error occurred. Ensure your 'system' folder contains files that start \
with 'nnnn', where the corresponding original file is called nnnn.txt. \
For example, the system-generated file corresponding to the original file 0001.txt
should begin with '0001'."""
        else:
            print "Finished."
    else:
        print """
batch_extract.py

Batch compares a folder of system-generated files with the original
files, so that 'differences' can be extracted as 'edits'.

Usage: python batch_extract.py <original> <system> <output>
original -- folder of original plain text (0001.txt).
system --- folder of system-generated edited plain text (0001XXN.txt).
output --- an output folder, which will contain 0001XXN.xml.

XX: Team ID.
N: Run number.

It is possible to have several 'runs' for the same source (original) file
processed in one go. ie. 0001.txt can be compared with 0001AB1.txt and 0001AB2.txt.
"""

