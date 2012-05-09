HOO 2012 Shared Task
Training Data Release 0.1
30th January 2012

Robert Dale [Robert.Dale@mq.edu.au]

This README file is associated with the first release of the HOO 2012
Shared Task Training Data, released on Monday 30th January 2012.

The data release comprises three elements:

-   Source:  A directory containing 1000 source files, each of which
    contains original text as written by subjects sitting the FCE
    exam.
-   Gold:  A directory containing 1000 gold standard edit sets
    corresponding to the source files; these specify corrections to
    preposition and determiner errors in the source data.
-   EvalTools: A directory containing evaluation tools for use with
    the above data.

All of these elements are documented on the HOO 2012 website at
www.correcttext.org/hoo2012. 

Note that this release of the data will be updated within a short
period of time --- probably around mid-January 2012 --- to correct any
identified errors. We seek feedback from the community on any problems
that require attention.  

Known Issues:

We are aware that there is a problem with insertions and deletions at
the beginning of sentences: since, strictly speaking, these edits
require a change in the casing of the following words, the gold
standard edits converted from the FCE data incorporate the
case-changed words as part of the corrections.  This will be fixed in
the next release of the data.

----End
