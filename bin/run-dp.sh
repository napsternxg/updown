#!/bin/bash

path=/data/deathpenalty/umd_deathpenalty_corpus/folds
out=out/data

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
        echo "\ttwok"
        updown run updown.preproc.impl.PreprocDPArticles --textPipeline "twokenize" -f $out/dp.twok.$fold.$mode.updown $path/$fold/$mode/*/*
        echo "\tbasic"
        updown run updown.preproc.impl.PreprocDPArticles --textPipeline "basicTokenize" -f $out/dp.basic.$fold.$mode.updown $path/$fold/$mode/*/*
      done
    done
    ;;
  eval)
    for fold in $path/*; do
      fold=$(basename $fold)
      for pipe in "twok" "basic" ; do
        for k in 25 50 75 100; do
          alpha=$( echo - | awk "{ print 50/$k }" )
          updown run updown.app.experiment.topic.lda.SplitLDAMaxentExperiment --numTopics $k --alpha $alpha --beta 0.01 --iterations 100 --name Dp_"$fold"_"$pipe"Lda$k -G $out/dp.$pipe.$fold.train.updown -g $out/dp.$pipe.$fold.test.updown
          updown run updown.app.experiment.topic.maxent.SplitMaxentExperiment --name Dp_"$fold"_"$pipe"Maxent -G $out/dp.$pipe.$fold.train.updown -g $out/dp.$pipe.$fold.test.updown
        done
      done
    done
    ;;
esac
