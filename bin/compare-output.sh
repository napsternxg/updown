#!/bin/bash

help()
{
cat <<EOF
This is a convenience script designed to run a series of experiments and compare
the result to known output.

The available sets of experiments are:
- emnlp
- reviews
- all
EOF
}

export CP_CONFIG=$UPDOWN_DIR/config/quiet

OUT=~/.updown_output
mkdir -p $OUT
OUTPUT=$OUT/output
GOLD=$OUT/gold

echo "Running the experiments defined in run-all.sh. This will take quite a while."

case $1 in
    emnlp) 
        OUTPUT=$OUT/output_emnlp
        GOLD=$OUT/gold_emnlp
        $UPDOWN_DIR/bin/experiments/run-emnlp2011.sh 2>&1 | tee $OUTPUT
        ;;
    reviews)
        OUTPUT=$OUT/output_reviews
        GOLD=$OUT/gold_reviews
        $UPDOWN_DIR/bin/experiments/run-reviews.sh 2>&1 | tee $OUTPUT
        ;;
    all)
        OUTPUT=$OUT/output_all
        GOLD=$OUT/gold_all
        $UPDOWN_DIR/bin/experiments/run-emnlp2011.sh 2>&1 | tee $OUTPUT
        $UPDOWN_DIR/bin/experiments/run-reviews.sh 2>&1 | tee -a $OUTPUT
        ;;
    *) echo "Unrecognized command: $CMD"; help; exit 1;;
esac

if [[ -e $GOLD ]]; then
    echo "Comparing the results against the last known good output ($GOLD)."
    RES=`diff $GOLD $OUTPUT`
    if [[ "$RES" != "" ]]; then
        echo "
        
        
        $RES"
        echo -n "Output has changed! Replace the known-good file? [yN] "
        read RESPONSE
        if [[ $RESPONSE == "y" ]]; then
            cp $OUTPUT $GOLD
        fi
    fi
else
    echo "
    
    No known good output. Using the current output for future comparisons ($GOLD)."
    cp $OUTPUT $GOLD
fi


echo -n "

Done! Save the output from this run? [yN] "
read RESPONSE
if [[ "$RESPONSE" == "y" ]]; then
    NEW_OUTPUT="$OUTOUT`date +%y%m%d%H%m%S`"
    cp $OUTPUT $NEW_OUTPUT
    echo "Output has been saved to $NEW_OUTPUT"
fi



