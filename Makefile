MPQA=src/main/resources/eng/lexicon/subjclueslen1polar.tff

all:
	updown build update compile

clean-test:
	rm $(MDSD_TEX_LEX) $(MDSD_TEX_ME) $(MDSD_TEX_NB) 2> /dev/null

# MDSD ---------------------------------------------------------------------------------------------------------------------

preproc-mdsd:
	#Books
	updown run updown.preproc.impl.PreprocMDSDReviews --textPipeline "basicTokenize"               -i /data/mdsd/processed_acl/books/unlabeled.review  -f out/data/mdsd.books.unlabeled.bigrams.updown
	updown run updown.preproc.impl.PreprocMDSDReviews --textPipeline "basicTokenize|filterBigrams" -i /data/mdsd/processed_acl/books/unlabeled.review  -f out/data/mdsd.books.unlabeled.noBigrams.updown
	#DVD
	updown run updown.preproc.impl.PreprocMDSDReviews --textPipeline "basicTokenize"               -i /data/mdsd/processed_acl/dvd/unlabeled.review  -f out/data/mdsd.dvd.unlabeled.bigrams.updown
	updown run updown.preproc.impl.PreprocMDSDReviews --textPipeline "basicTokenize|filterBigrams" -i /data/mdsd/processed_acl/dvd/unlabeled.review  -f out/data/mdsd.dvd.unlabeled.noBigrams.updown
	#Electronics
	updown run updown.preproc.impl.PreprocMDSDReviews --textPipeline "basicTokenize"               -i /data/mdsd/processed_acl/electronics/unlabeled.review  -f out/data/mdsd.electronics.unlabeled.bigrams.updown
	updown run updown.preproc.impl.PreprocMDSDReviews --textPipeline "basicTokenize|filterBigrams" -i /data/mdsd/processed_acl/electronics/unlabeled.review  -f out/data/mdsd.electronics.unlabeled.noBigrams.updown
	#Kitchen
	updown run updown.preproc.impl.PreprocMDSDReviews --textPipeline "basicTokenize"               -i /data/mdsd/processed_acl/kitchen/unlabeled.review  -f out/data/mdsd.kitchen.unlabeled.bigrams.updown
	updown run updown.preproc.impl.PreprocMDSDReviews --textPipeline "basicTokenize|filterBigrams" -i /data/mdsd/processed_acl/kitchen/unlabeled.review  -f out/data/mdsd.kitchen.unlabeled.noBigrams.updown

MDSD_TEX=out/report/test-mdsd.tex
MDSD_TEX_LEX=out/report/each/mdsd.lex.tex
$(MDSD_TEX_LEX): $(MPQA)
	#Books
	updown run updown.app.experiment.lexical.LexicalRatioExperiment --format tex --name LexMdsdBooksBi --mpqa $(MPQA) -g out/data/mdsd.books.unlabeled.bigrams.updown > $(MDSD_TEX_LEX)
	updown run updown.app.experiment.lexical.LexicalRatioExperiment --format tex --name LexMdsdBooks --mpqa $(MPQA) -g out/data/mdsd.books.unlabeled.noBigrams.updown >> $(MDSD_TEX_LEX)
	#DVD
	updown run updown.app.experiment.lexical.LexicalRatioExperiment --format tex --name LexMdsdDvdBi --mpqa $(MPQA) -g out/data/mdsd.dvd.unlabeled.bigrams.updown >> $(MDSD_TEX_LEX)
	updown run updown.app.experiment.lexical.LexicalRatioExperiment --format tex --name LexMdsdDvd --mpqa $(MPQA) -g out/data/mdsd.dvd.unlabeled.noBigrams.updown >> $(MDSD_TEX_LEX)
	#Electronics
	updown run updown.app.experiment.lexical.LexicalRatioExperiment --format tex --name LexMdsdElecBi --mpqa $(MPQA) -g out/data/mdsd.electronics.unlabeled.bigrams.updown >> $(MDSD_TEX_LEX)
	updown run updown.app.experiment.lexical.LexicalRatioExperiment --format tex --name LexMdsdElec --mpqa $(MPQA) -g out/data/mdsd.electronics.unlabeled.noBigrams.updown >> $(MDSD_TEX_LEX)
	#Kitchen
	updown run updown.app.experiment.lexical.LexicalRatioExperiment --format tex --name LexMdsdKitchen --mpqa $(MPQA) -g out/data/mdsd.kitchen.unlabeled.bigrams.updown >> $(MDSD_TEX_LEX)
	updown run updown.app.experiment.lexical.LexicalRatioExperiment --format tex --name LexMdsdKitchenBi --mpqa $(MPQA) -g out/data/mdsd.kitchen.unlabeled.noBigrams.updown >> $(MDSD_TEX_LEX)

MDSD_TEX_ME=out/report/each/mdsd.me.tex
$(MDSD_TEX_ME): $(MPQA)
	#Books
	updown run updown.app.experiment.maxent.NFoldMaxentExperiment -n 10 --format tex --name MeMdsdBooksBi -g out/data/mdsd.books.unlabeled.bigrams.updown > $(MDSD_TEX_ME)
	updown run updown.app.experiment.maxent.NFoldMaxentExperiment -n 10 --format tex --name MeMdsdBooks -g out/data/mdsd.books.unlabeled.noBigrams.updown >> $(MDSD_TEX_ME)
	#DVD
	updown run updown.app.experiment.maxent.NFoldMaxentExperiment -n 10 --format tex --name MeMdsdDvdBi -g out/data/mdsd.dvd.unlabeled.bigrams.updown >> $(MDSD_TEX_ME)
	updown run updown.app.experiment.maxent.NFoldMaxentExperiment -n 10 --format tex --name MeMdsdDvd -g out/data/mdsd.dvd.unlabeled.noBigrams.updown >> $(MDSD_TEX_ME)
	#Electronics
	updown run updown.app.experiment.maxent.NFoldMaxentExperiment -n 10 --format tex --name MeMdsdElecBi -g out/data/mdsd.electronics.unlabeled.bigrams.updown >> $(MDSD_TEX_ME)
	updown run updown.app.experiment.maxent.NFoldMaxentExperiment -n 10 --format tex --name MeMdsdElec -g out/data/mdsd.electronics.unlabeled.noBigrams.updown >> $(MDSD_TEX_ME)
	#Kitchen
	updown run updown.app.experiment.maxent.NFoldMaxentExperiment -n 10 --format tex --name MeMdsdKitchenBi -g out/data/mdsd.kitchen.unlabeled.bigrams.updown >> $(MDSD_TEX_ME)
	updown run updown.app.experiment.maxent.NFoldMaxentExperiment -n 10 --format tex --name MeMdsdKitchen -g out/data/mdsd.kitchen.unlabeled.noBigrams.updown >> $(MDSD_TEX_ME)

MDSD_TEX_NB=out/report/each/mdsd.nb.tex
$(MDSD_TEX_NB): $(MPQA)
	#Books
	updown run updown.app.experiment.nbayes.NFoldNBayesExperiment -n 10 --format tex --name NbMdsdBooksBi -g out/data/mdsd.books.unlabeled.bigrams.updown > $(MDSD_TEX_NB)
	updown run updown.app.experiment.nbayes.NFoldNBayesExperiment -n 10 --format tex --name NbMdsdBooks -g out/data/mdsd.books.unlabeled.noBigrams.updown >> $(MDSD_TEX_NB)
	#DVD
	updown run updown.app.experiment.nbayes.NFoldNBayesExperiment -n 10 --format tex --name NbMdsdDvdBi -g out/data/mdsd.dvd.unlabeled.bigrams.updown >> $(MDSD_TEX_NB)
	updown run updown.app.experiment.nbayes.NFoldNBayesExperiment -n 10 --format tex --name NbMdsdDvd -g out/data/mdsd.dvd.unlabeled.noBigrams.updown >> $(MDSD_TEX_NB)
	#Electronics
	updown run updown.app.experiment.nbayes.NFoldNBayesExperiment -n 10 --format tex --name NbMdsdElecBi -g out/data/mdsd.electronics.unlabeled.bigrams.updown >> $(MDSD_TEX_NB)
	updown run updown.app.experiment.nbayes.NFoldNBayesExperiment -n 10 --format tex --name NbMdsdElec -g out/data/mdsd.electronics.unlabeled.noBigrams.updown >> $(MDSD_TEX_NB)
	#Kitchen
	updown run updown.app.experiment.nbayes.NFoldNBayesExperiment -n 10 --format tex --name NbMdsdKitchenBi -g out/data/mdsd.kitchen.unlabeled.bigrams.updown >> $(MDSD_TEX_NB)
	updown run updown.app.experiment.nbayes.NFoldNBayesExperiment -n 10 --format tex --name NbMdsdKitchen -g out/data/mdsd.kitchen.unlabeled.noBigrams.updown >> $(MDSD_TEX_NB)

$(MDSD_TEX): $(MDSD_TEX_LEX) $(MDSD_TEX_ME) $(MDSD_TEX_NB)
	cat $(MDSD_TEX_LEX) $(MDSD_TEX_ME) $(MDSD_TEX_NB) > $(MDSD_TEX)

test-mdsd.tex: $(MDSD_TEX)

# HCR ---------------------------------------------------------------------------------------------------------------------

preproc-hcr:
	updown run updown.preproc.impl.PreprocHCRTweets --textPipeline "basicTokenize|addBigrams|removeStopwords" -i /data/hcr/train/orig/hcr-train.csv  -f out/data/hcr.train.bigrams.updown -t out/data/hcr.train.targets
	updown run updown.preproc.impl.PreprocHCRTweets --textPipeline "basicTokenize|removeStopwords"            -i /data/hcr/train/orig/hcr-train.csv  -f out/data/hcr.train.noBigrams.updown -t out/data/hcr.train.targets
	updown run updown.preproc.impl.PreprocHCRTweets --textPipeline "basicTokenize|addBigrams|removeStopwords" -i /data/hcr/dev/orig/hcr-dev.csv  -f out/data/hcr.dev.bigrams.updown -t out/data/hcr.dev.targets
	updown run updown.preproc.impl.PreprocHCRTweets --textPipeline "basicTokenize|removeStopwords"            -i /data/hcr/dev/orig/hcr-dev.csv  -f out/data/hcr.dev.noBigrams.updown -t out/data/hcr.dev.targets
	cat out/data/hcr.train.bigrams.updown out/data/hcr.dev.bigrams.updown > out/data/hcr.train_dev.bigrams.updown
	cat out/data/hcr.train.noBigrams.updown out/data/hcr.dev.noBigrams.updown > out/data/hcr.train_dev.noBigrams.updown


HCR_TEX=out/report/test-hcr.tex
HCR_TEX_LEX=out/report/each/hcr.lex.tex
$(HCR_TEX_LEX): $(MPQA)
	updown run updown.app.experiment.lexical.LexicalRatioExperiment --format tex --name LexHcrBi --mpqa $(MPQA) -g out/data/hcr.train_dev.bigrams.updown > $(HCR_TEX_LEX)
	updown run updown.app.experiment.lexical.LexicalRatioExperiment --format tex --name LexHcr --mpqa $(MPQA) -g out/data/hcr.train_dev.noBigrams.updown >> $(HCR_TEX_LEX)

HCR_TEX_ME=out/report/each/hcr.me.tex
$(HCR_TEX_ME): $(MPQA)
	updown run updown.app.experiment.maxent.NFoldMaxentExperiment -n 10 --format tex --name MeHcrBi -g out/data/hcr.train_dev.bigrams.updown > $(HCR_TEX_ME)
	updown run updown.app.experiment.maxent.NFoldMaxentExperiment -n 10 --format tex --name MeHcr -g out/data/hcr.train_dev.noBigrams.updown >> $(HCR_TEX_ME)

HCR_TEX_NB=out/report/each/hcr.nb.tex
$(HCR_TEX_NB): $(MPQA)
	updown run updown.app.experiment.nbayes.NFoldNBayesExperiment -n 10 --format tex --name NbHcrBi -g out/data/hcr.train_dev.bigrams.updown > $(HCR_TEX_NB)
	updown run updown.app.experiment.nbayes.NFoldNBayesExperiment -n 10 --format tex --name NbHcr -g out/data/hcr.train_dev.noBigrams.updown >> $(HCR_TEX_NB)

$(HCR_TEX): $(HCR_TEX_LEX) $(HCR_TEX_ME) $(HCR_TEX_NB)
	cat $(HCR_TEX_LEX) $(HCR_TEX_ME) $(HCR_TEX_NB) > $(HCR_TEX)

test-hcr.tex: $(HCR_TEX)

