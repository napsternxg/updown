#!/bin/bash

iter=1000
beta=0.01
nFold=3
for k in 3 10 100; do \
  alpha=$( echo - | awk "{ print 50/$k }" )
  
  echo Maxent k=$k
  updown run updown.app.experiment.topic.lda.NFoldDiscriminantLDAExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --name Lda"$k"MaxentHcrBi -g $UPDOWN_DIR/out/data/hcr.train_dev.unlabeled.bigrams.updown 
  updown run updown.app.experiment.topic.lda.NFoldDiscriminantLDAExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --name Lda"$k"MaxentHcr -g $UPDOWN_DIR/out/data/hcr.train_dev.unlabeled.noBigrams.updown 
  
  for KNN in 5 11 21; do \
    echo iter=$iter alpha=$alpha beta=$beta k=$k KNN=$KNN name=Lda"$k"Knn"$KNN"Hcr
    date
    
    updown run updown.app.experiment.topic.lda.NFoldKNNDiscriminantExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --numNearestNeighbors $KNN --name Lda"$k"Knn"$KNN"HcrBi -g $UPDOWN_DIR/out/data/hcr.train_dev.unlabeled.bigrams.updown 
    updown run updown.app.experiment.topic.lda.NFoldKNNDiscriminantExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --numNearestNeighbors $KNN --name Lda"$k"Knn"$KNN"Hcr -g $UPDOWN_DIR/out/data/hcr.train_dev.unlabeled.noBigrams.updown 
    
  done
done
