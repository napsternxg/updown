#!/bin/bash

path=/data/mdsd/processed_acl
out=out/data
stoplist=src/main/resources/eng/dictionary/stoplist.txt

CMD=$1
shift

case $CMD in 
  preproc)
    for domain in $path/*; do
      domain=$(basename $domain)
      outfile=$out/mdsd.$domain.combined.updown
      echo generating $outfile
      for pipe in "twok" "twok_stop" "twok_1600V" "basic" "basic_stop" "basic_1600V"; do
        case $pipe in
          twok) 
            pipeline="twokenize"
            vocab=""
            ;;
          twok_stop) 
            pipeline="twokenize|removeStopwords"
            vocab=""
            ;;
          twok_1600V)
            pipeline="twokenize"
            vocab="--vocabSize 1600"
            ;;
          basic) 
            pipeline="basicTokenize"
            vocab=""
            ;;
          basic_stop) 
            pipeline="basicTokenize|removeStopwords"
            vocab=""
            ;;
          basic_1600V)
            pipeline="basicTokenize"
            vocab="--vocabSize 1600"
            ;;
        esac
        echo "STAT: $domain $mode $pipe"
        echo updown run updown.preproc.impl.PreprocMDSDReviews --textPipeline $pipeline $vocab -f $out/mdsd.$pipe.$domain.combined.updown $path/$domain/*
        updown run updown.preproc.impl.PreprocMDSDReviews --textPipeline $pipeline $vocab -f $out/mdsd.$pipe.$domain.combined.updown $path/$domain/*
        cat $out/mdsd.$pipe.$domain.combined.updown | ~/repos/updown/make_split.py $out/mdsd.$pipe.$domain.train.updown $out/mdsd.$pipe.$domain.test.updown
        wc -l $out/mdsd.$pipe.$domain.train.updown $out/mdsd.$pipe.$domain.test.updown

      done
    done
    ;;
  eval)
    for domain in $path/*; do
      domain=$(basename $domain)
      for pipe in "twok" "twok_stop" "twok_1600V" "basic" "basic_stop" "basic_1600V"; do
        TRAIN="-G $out/mdsd.$pipe.$domain.train.updown -G $out/../nodistribute/bb7_noNeut"
        TEST="-g $out/mdsd.$pipe.$domain.test.updown"
        for k in 25 50 75 100; do
          alpha=$( echo - | awk "{ print 50/$k }" )
          echo "STAT: $domain $pipe lda $k"
          updown 3 run updown.app.experiment.topic.lda.SplitLDAMaxentExperiment --numTopics $k --alpha $alpha --beta 0.01 --iterations 100  $TRAIN $TEST
        done
        echo "STAT: $domain $pipe maxent"
        updown run updown.app.experiment.maxent.SplitMaxentExperiment  $TRAIN $TEST
      done
    done
    ;;
esac
