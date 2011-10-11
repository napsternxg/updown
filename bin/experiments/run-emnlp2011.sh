#!/bin/bash

OUTPUT=/tmp/updown
mkdir -p $OUTPUT
zcat $UPDOWN_DIR/models/maxent-eng.mxm.gz > $OUTPUT/maxent-eng.mxm

echo "

PREPROC Stanford" 1>&2
updown preproc-stanford -i $UPDOWN_DIR/data/stanford/orig/testdata.manual.2009.05.25  -s $UPDOWN_DIR/src/main/resources/eng/dictionary/stoplist.txt > $OUTPUT/stanford-features.txt
echo "

PREPROC Shamma" 1>&2
updown preproc-shamma   -i $UPDOWN_DIR/data/shamma/orig/debate08_sentiment_tweets.tsv -s $UPDOWN_DIR/src/main/resources/eng/dictionary/stoplist.txt > $OUTPUT/shamma-features.txt
echo "

PREPROC HCR Train" 1>&2
updown preproc-hcr      -i $UPDOWN_DIR/data/hcr/train/orig/hcr-train.csv              -s $UPDOWN_DIR/src/main/resources/eng/dictionary/stoplist.txt -t $OUTPUT/hcr-train-targets.txt > $OUTPUT/hcr-train-features.txt
echo "

PREPROC HCR Dev" 1>&2
updown preproc-hcr      -i $UPDOWN_DIR/data/hcr/dev/orig/hcr-dev.csv                  -s $UPDOWN_DIR/src/main/resources/eng/dictionary/stoplist.txt -t $OUTPUT/hcr-dev-targets.txt > $OUTPUT/hcr-dev-features.txt
echo "

PREPROC HCR Test" 1>&2
updown preproc-hcr      -i $UPDOWN_DIR/data/hcr/test/orig/hcr-test.csv                -s $UPDOWN_DIR/src/main/resources/eng/dictionary/stoplist.txt -t $OUTPUT/hcr-test-targets.txt > $OUTPUT/hcr-test-features.txt

echo "

lex-ratio" 1>&2
updown lex-ratio        -g $OUTPUT/stanford-features.txt -p $UPDOWN_DIR/src/main/resources/eng/lexicon/subjclueslen1polar.tff
echo "

per-tweet-eval" 1>&2
updown per-tweet-eval   -g $OUTPUT/stanford-features.txt -m $OUTPUT/maxent-eng.mxm
echo "

per-user-eval" 1>&2
updown per-user-eval    -g $OUTPUT/stanford-features.txt -m $OUTPUT/maxent-eng.mxm

MEM=4
echo "

junto" 1>&2
updown $MEM junto          -g $OUTPUT/stanford-features.txt -m $OUTPUT/maxent-eng.mxm -p $UPDOWN_DIR/src/main/resources/eng/lexicon/subjclueslen1polar.tff -f $UPDOWN_DIR/data/stanford/username-username-edges.txt -r $UPDOWN_DIR/src/main/resources/eng/model/ngramProbs.ser.gz > /dev/null
echo "

follower-graph" 1>&2
updown $MEM junto          -g $OUTPUT/stanford-features.txt -m $OUTPUT/maxent-eng.mxm -p $UPDOWN_DIR/src/main/resources/eng/lexicon/subjclueslen1polar.tff -f $UPDOWN_DIR/data/stanford/username-username-edges.txt -r $UPDOWN_DIR/src/main/resources/eng/model/ngramProbs.ser.gz -e fm > /dev/null
echo "

targets" 1>&2
updown $MEM junto          -g $OUTPUT/hcr-dev-features.txt  -m $OUTPUT/maxent-eng.mxm -p $UPDOWN_DIR/src/main/resources/eng/lexicon/subjclueslen1polar.tff -f $UPDOWN_DIR/data/hcr/username-username-edges.txt      -r $UPDOWN_DIR/src/main/resources/eng/model/ngramProbs.ser.gz -t $OUTPUT/hcr-dev-targets.txt > /dev/null
