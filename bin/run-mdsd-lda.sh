#!/bin/bash

iter=1000
beta=0.01
nFold=3
for k in 100; do \
  alpha=$( echo - | awk "{ print 50/$k }" )
  
  updown run updown.app.experiment.topic.lda.NFoldDiscriminantLDAExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --name Lda"$k"MaxentMdsdBooksBi -g $UPDOWN_DIR/out/data/mdsd.books.unlabeled.bigrams.updown 
  updown run updown.app.experiment.topic.lda.NFoldDiscriminantLDAExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --name Lda"$k"MaxentMdsdBooks -g $UPDOWN_DIR/out/data/mdsd.books.unlabeled.noBigrams.updown 
  
  updown run updown.app.experiment.topic.lda.NFoldDiscriminantLDAExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --name Lda"$k"MaxentMdsdDvdBi -g $UPDOWN_DIR/out/data/mdsd.dvd.unlabeled.bigrams.updown 
  updown run updown.app.experiment.topic.lda.NFoldDiscriminantLDAExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --name Lda"$k"MaxentMdsdDvd -g $UPDOWN_DIR/out/data/mdsd.dvd.unlabeled.noBigrams.updown 
  
  updown run updown.app.experiment.topic.lda.NFoldDiscriminantLDAExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --name Lda"$k"MaxentMdsdElecBi -g $UPDOWN_DIR/out/data/mdsd.electronics.unlabeled.bigrams.updown 
  updown run updown.app.experiment.topic.lda.NFoldDiscriminantLDAExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --name Lda"$k"MaxentMdsdElec -g $UPDOWN_DIR/out/data/mdsd.electronics.unlabeled.noBigrams.updown 
  
  updown run updown.app.experiment.topic.lda.NFoldDiscriminantLDAExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --name Lda"$k"MaxentMdsdKitchenBi -g $UPDOWN_DIR/out/data/mdsd.kitchen.unlabeled.bigrams.updown 
  updown run updown.app.experiment.topic.lda.NFoldDiscriminantLDAExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --name Lda"$k"MaxentMdsdKitchen -g $UPDOWN_DIR/out/data/mdsd.kitchen.unlabeled.noBigrams.updown 
  
  for KNN in 5 11 21; do \
    echo iter=$iter alpha=$alpha beta=$beta k=$k KNN=$KNN name=Lda"$k"Knn"$KNN"MdsdBooks
    date
    
    updown run updown.app.experiment.topic.lda.NFoldKNNDiscriminantExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --numNearestNeighbors $KNN --name Lda"$k"Knn"$KNN"MdsdBooksBi -g $UPDOWN_DIR/out/data/mdsd.books.unlabeled.bigrams.updown 
    updown run updown.app.experiment.topic.lda.NFoldKNNDiscriminantExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --numNearestNeighbors $KNN --name Lda"$k"Knn"$KNN"MdsdBooks -g $UPDOWN_DIR/out/data/mdsd.books.unlabeled.noBigrams.updown 
    
    updown run updown.app.experiment.topic.lda.NFoldKNNDiscriminantExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --numNearestNeighbors $KNN --name Lda"$k"Knn"$KNN"MdsdDvdBi -g $UPDOWN_DIR/out/data/mdsd.dvd.unlabeled.bigrams.updown 
    updown run updown.app.experiment.topic.lda.NFoldKNNDiscriminantExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --numNearestNeighbors $KNN --name Lda"$k"Knn"$KNN"MdsdDvd -g $UPDOWN_DIR/out/data/mdsd.dvd.unlabeled.noBigrams.updown 
    
    updown run updown.app.experiment.topic.lda.NFoldKNNDiscriminantExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --numNearestNeighbors $KNN --name Lda"$k"Knn"$KNN"MdsdElecBi -g $UPDOWN_DIR/out/data/mdsd.electronics.unlabeled.bigrams.updown 
    updown run updown.app.experiment.topic.lda.NFoldKNNDiscriminantExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --numNearestNeighbors $KNN --name Lda"$k"Knn"$KNN"MdsdElec -g $UPDOWN_DIR/out/data/mdsd.electronics.unlabeled.noBigrams.updown 
    
    updown run updown.app.experiment.topic.lda.NFoldKNNDiscriminantExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --numNearestNeighbors $KNN --name Lda"$k"Knn"$KNN"MdsdKitchenBi -g $UPDOWN_DIR/out/data/mdsd.kitchen.unlabeled.bigrams.updown 
    updown run updown.app.experiment.topic.lda.NFoldKNNDiscriminantExperiment -n $nFold --format tex --alpha $alpha --beta $beta --iterations $iter --numTopics $k --numNearestNeighbors $KNN --name Lda"$k"Knn"$KNN"MdsdKitchen -g $UPDOWN_DIR/out/data/mdsd.kitchen.unlabeled.noBigrams.updown 
  done
done
