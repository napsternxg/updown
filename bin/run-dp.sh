#!/bin/bash

path=/data/deathpenalty/umd_deathpenalty_corpus/folds
out=out/data
stoplist=src/main/resources/eng/dictionary/stoplist.txt

CMD=$1
shift

case $CMD in 
  preproc)
    for fold in $path/*; do
      for mode in "test" "train"; do
        fold=$(basename $fold)
        mode=$(basename $mode)
        outfile=$out/dp.$fold.$mode.updown
        echo "generating $outfile"
        echo "STAT: $fold $mode twok_1600V"
        updown run updown.preproc.impl.PreprocDPArticles --textPipeline "twokenize" --vocabSize 1600 -f $out/dp.twok_1600V.$fold.$mode.updown $path/$fold/$mode/*/*
        echo "STAT: $fold $mode basic_1600V"
        updown run updown.preproc.impl.PreprocDPArticles --textPipeline "basicTokenize" --vocabSize 1600 -f $out/dp.basic_1600V.$fold.$mode.updown $path/$fold/$mode/*/*
      done
    done
    ;;
  eval)
    for fold in $path/*; do
      fold=$(basename $fold)
      for pipe in "twok" "twok_stop" "twok_1600V" "basic" "basic_stop" "basic_1600V"; do
        for k in 25 50 75 100; do
          alpha=$( echo - | awk "{ print 50/$k }" )
          echo "STAT: $fold $pipe lda $k"
          java -Xmx3g -jar ~/repos/updown/target/updown-0.1.0-one-jar.jar experiment split lda-maxent --numTopics $k --alpha $alpha --beta 0.01 --iterations 100 --name Dp_"$fold"_"$pipe"Lda$k -G $out/dp.$pipe.$fold.train.updown -g $out/dp.$pipe.$fold.test.updown
        done
        sigma=0.0
        echo "STAT: $fold $pipe maxent-sig$sigma"
        java -jar ~/repos/updown/target/updown-0.1.0-one-jar.jar experiment split maxent --iterations 100 --sigma $sigma -G $out/dp.$pipe.$fold.train.updown -g $out/dp.$pipe.$fold.test.updown
      done
    done
    ;;
esac
