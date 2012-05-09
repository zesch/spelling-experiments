# batch_evaluate.py
#
# Author:	George Narroway 
# Date:		10 August 2011
# Version:      2.2
#
# Copyright (c) Centre for Language Technology, Macquarie University
# Not to be used for commercial purposes.
#
# Batch evaluates two folders of edits corresponding to gold standard,
# and system-generated output respectively. The output is consolidated
# into a CSV file, giving scores for each file as well as overall average.
#
# Usage: python batch_evaluate.py [-t] gold_folder sys_folder output.csv
#
# -t --type -- outputs scores grouped by type.
# gold_folder -- a folder containing gold standard edits in XML format
#		(0001GE.XML).  
# sys_folder -- a folder of system-generated edits in XML format
#		(0001XXN.XML). 
# output.csv -- a path to a output file.
#
# XX -- Team ID.
# N -- Run number.
#
# It is possible to have several 'runs' for the same source (original)
# file processed in one go: e.g., 0001GE.xml can be compared with
# 0001AB1.xml and 0001AB2.xml. 

import sys
import re
import evaluate
import os
import csv
import traceback
from optparse import OptionParser
reload(evaluate)


TYPES =('detection', 'recognition', 'correction')
VALUES = ('precision', 'recall', 'score')


def compile_files(root):
    """Generates a list of full paths corresponding 
    to the files in a given source directory.
    """
    files = [os.path.join(root, f) for f in os.listdir(root) if not f.startswith(".")]
    
    return files


def flatten(s):
    """Decomposes dictionaries to yield a flat list of scores."""
    return [s[t][v] for t in TYPES for v in VALUES]


def file_name(path):
    """Gets the filename sans extension from a file path."""
    return os.path.basename(path).split('.')[0]


def base_name(path):
    """Convenience function to return basename."""
    return os.path.basename(path)


def percentage(x, y):
    """Gets x over y as a percentage."""
    try:
        return 100 * (float(x) / y)
    except ZeroDivisionError:
        return "undefined"

    
def write_scores(path, lines):
    """Makes a header and writes everything to csv."""

    headers = ["File"] + ["%s%s" % (t,v) for t in TYPES for v in VALUES]
    write_csv(path, lines, headers)
    

def write_type_scores(path, lines):
    """Makes a header and writes everything to csv."""

    print "Opening %s for score output" % base_name(path)
    headers = ["Type", "Total", "Detection", "Detection (%)",
               "Recognition", "Recognition (%)",
               "Correction", "Correction (%)"]

    write_csv(path, lines, headers)


def write_csv(path, lines, headers):
    """Writes a header and subsequent data to a csv file."""
    print "Opening %s for score output" % base_name(path)

    try:
        f = open(path, 'wb')
        writer = csv.writer(f)
        writer.writerow(headers)
        writer.writerows(lines)
    except IOError:
        print "Cannot open %s" % path
    else:
        print "Scores successfully written to %s" % path
        f.close()


def match_files(gold_folder, sys_folder):
    """Find pairs of corresponding files.
    Gold files are of the form '0001GE.xml'.
    Corresponding sys files must start with '0001'."""

    print "Compiling files..."
    # Get a list of files in the folders supplied.
    gold_files = compile_files(gold_folder) # nnnnG.xml
    sys_files = compile_files(sys_folder)   # nnnnXXN.xml

    print "%d gold files found in %s" % (len(gold_files), base_name(gold_folder))
    print "%d system files found in %s\n" % (len(sys_files), base_name(sys_folder))

    print "Matching system files to gold files..."
    # Match them up, where nnnn must be common in a pair.
    pairs = [(f1, f2) for f1 in gold_files for f2 in sys_files
             if base_name(f2).startswith(base_name(f1).split("GE.")[0])]

    return pairs


def output_scores(pairs, out_file, bonus=True):
    """Score pairs of files and write result to CSV."""

    print "\nEvaluating system output..."
    # Retrieve the scores for each pair, maintaining file name.
    scores = [(file_name(p2), evaluate.process(p1, p2, bonus)) for (p1, p2) in pairs]

    # Flatten the scores into a single list, keeping file name.
    flat_scores = [(f, flatten(s)) for (f,s) in scores]
    flat_scores.sort(key = lambda x: x[0])

    # Append the file name to create a csv row.
    lines = [[f] + s for f,s in flat_scores]

    # Compute averages for each column of scores.
    averages = [sum(s[i] for f,s in flat_scores)/len(flat_scores) for i in range(9)]
    lines.append(["Average"] + averages)

    # Write out.
    write_scores(out_file, lines)


def output_type_scores(pairs, out_file, mode="total"):
    """Score pairs of files and write result to CSV. Outputs by type,
    the number and proportions of correct detections, recognitions and
    corrections."""

    print "\nEvaluating system output by type..."
    # Retrieve the scores for each pair, maintaining file name.
    # score[k] = (total, detections, recogntions, corrections)
    scores = [evaluate.process_type(p1, p2, mode) for (p1, p2) in pairs]

    count = dict()
    detected = dict()
    recognised = dict()
    corrected = dict()

    for score in scores:
        for t,v in score.iteritems():
            count[t] = count.get(t, 0) + v[0]
            detected[t] = detected.get(t, 0) + v[1]
            recognised[t] = recognised.get(t, 0) + v[2]
            corrected[t] = corrected.get(t, 0) + v[3]

    lines = [[k, count[k],
              detected[k], percentage(detected[k], count[k]),
              recognised[k], percentage(recognised[k], count[k]),
              corrected[k], percentage(corrected[k], count[k])]
             for k in sorted(count.iterkeys())]

    # Write out.
    write_type_scores(out_file, lines)

    
def print_usage():
    print """
# batch_evaluate.py

# Batch evaluates two folders of edits corresponding to gold standard,
# and system-generated output respectively. The output is consolidated
# into a CSV file, giving scores for each file as well as overall average.
#
# Usage: python batch_evaluate.py [-t] gold_folder sys_folder output.csv
#
# -t --type -- outputs proportions scores grouped by type.
# gold_folder -- a folder containing gold standard edits in XML format (0001GE.XML).
# sys_folder -- a folder of system-generated edits in XML format (0001XXN.XML).
# output.csv -- a path to a output file.
#
# XX -- Team ID.
# N -- Run number.
#
# It is possible to have several 'runs' for the same source (original) file
# processed in one go: e.g. 0001GE.xml can be compared with 0001AB1.xml
# and 0001AB2.xml. 
"""

if __name__ == '__main__':
    parser = OptionParser()
    parser.add_option("-t", action="store_true", dest="by_type")
    (options, args) = parser.parse_args()

    if len(args) == 3:
        gold_folder = args[0]
        sys_folder = args[1]
        out_file = args[2]
        
        try:
            pairs = match_files(gold_folder, sys_folder)

            if pairs:
                print "%d pairs found" % len(pairs)
                if options.by_type:
                    path_components = out_file.rsplit(".", 1)
                    # Required edits only
                    out_file = "_required.".join(path_components)
                    output_type_scores(pairs, out_file, mode="required")

                    # Optional edits only
                    out_file = "_optional.".join(path_components)
                    output_type_scores(pairs, out_file, mode="optional")

                    # All edits
                    out_file = "_total.".join(path_components)
                    output_type_scores(pairs, out_file, mode="total")
                else:
                    # Include bonus for missed optional edits
                    output_scores(pairs, out_file, bonus=True)

                    # Add suffix to file name before extension
                    path_components = out_file.rsplit(".", 1)
                    out_file2 = "_nobonus.".join(path_components)

                    # Don't include bonus for missed optional edits
                    output_scores(pairs, out_file2, bonus=False)
            else:
                raise Exception("no pairs")
        except:
            print """An error occurred. Ensure your 'system' folder contains files that include
nnnn in the file name, where the corresponding gold file would be named nnnnGE.xml. \
For example, the systme-generated file corresponding to a gold file named 0001GE.xml
should begin with '0001'."""
            traceback.print_exc()
    else:
        print_usage()       

