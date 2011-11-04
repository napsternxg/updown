#!/bin/bash

OUTPUT=/tmp/updown
mkdir -p $OUTPUT

# polarity corpus ===============================================
echo "

preproc polarity"
updown run updown.preproc.PreprocPangLeePolarityCorpus -i "/data/pang_lee_polarity_dataset_v2.0/0/pos->pos,/data/pang_lee_polarity_dataset_v2.0/0/neg->neg" > $OUTPUT/polarity_corpus.txt

echo "

10-fold maxent on polarity corpus" 1>&2
updown run updown.app.NFoldMaxentExperiment -g $OUTPUT/polarity_corpus.txt > /dev/null

echo "

preproc polarity bigrams"
updown run updown.preproc.PreprocPangLeePolarityCorpus -i "/data/pang_lee_polarity_dataset_v2.0/0/pos->pos,/data/pang_lee_polarity_dataset_v2.0/0/neg->neg" --textPipeline "splitSpace|addBiGrams|twokenizeSkipGtOneGrams|removeStopwords" > $OUTPUT/polarity_corpus_bi.txt

echo "

10-fold maxent on polarity corpus with bigrams" 1>&2
updown run updown.app.NFoldMaxentExperiment -g $OUTPUT/polarity_corpus_bi.txt > /dev/null


# sentence corpus ===============================================
echo "

preproc sentence"
updown run updown.preproc.PreprocPangLeeSentenceCorpus -i "/data/pang_lee_sentence_corpus/0/rt-polarity.neg->neg,/data/pang_lee_sentence_corpus/0/rt-polarity.pos->pos" > $OUTPUT/sentence_corpus.txt
echo "

10-fold maxent on sentence corpus" 1>&2
updown run updown.app.NFoldMaxentExperiment -g $OUTPUT/sentence_corpus.txt > /dev/null

echo "

preproc sentence bigrams"
updown run updown.preproc.PreprocPangLeeSentenceCorpus -i "/data/pang_lee_sentence_corpus/0/rt-polarity.neg->neg,/data/pang_lee_sentence_corpus/0/rt-polarity.pos->pos" --textPipeline "splitSpace|addBiGrams|twokenizeSkipGtOneGrams|removeStopwords" > $OUTPUT/sentence_corpus_bi.txt
echo "

10-fold maxent on sentence corpus with bigrams" 1>&2
updown run updown.app.NFoldMaxentExperiment -g $OUTPUT/sentence_corpus_bi.txt > /dev/null


# scale corpus ===============================================
echo "

preproc scale data"
updown run updown.preproc.PreprocPangLeeSentenceCorpus -i "/data/pang_lee_scale_data/1/0->neg,/data/pang_lee_scale_data/1/1->neu,/data/pang_lee_scale_data/1/2->pos" > $OUTPUT/scale_corpus_3.txt

echo "

10-fold maxent on 3 point scale corpus" 1>&2
updown run updown.app.NFoldMaxentExperiment -g $OUTPUT/scale_corpus_3.txt > /dev/null


echo "

preproc scale data bigrams"
updown run updown.preproc.PreprocPangLeeSentenceCorpus -i "/data/pang_lee_scale_data/1/0->neg,/data/pang_lee_scale_data/1/1->neu,/data/pang_lee_scale_data/1/2->pos" --textPipeline "splitSpace|addBiGrams|twokenizeSkipGtOneGrams|removeStopwords" > $OUTPUT/scale_corpus_3_bi.txt

echo "

10-fold maxent on 3 point scale corpus with bigrams" 1>&2
updown run updown.app.NFoldMaxentExperiment -g $OUTPUT/scale_corpus_3_bi.txt > /dev/null
