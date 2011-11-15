package updown.app

import org.clapper.argot.ArgotConverters._
import org.clapper.argot.ArgotParser._
import org.clapper.argot.{ArgotUsageException, ArgotParser}
import updown.data.io.TweetFeatureReader
import opennlp.tools.chunker.{ChunkerModel, ChunkerME}
import java.io._
import updown.preproc.impl.PreprocTSVFilesCat
import updown.data.{GoldLabeledTweet, SentimentLabel}
import com.weiglewilczek.slf4s.Logging
import opennlp.tools.sentdetect.{SentenceModel, SentenceDetectorME, SentenceDetector}
import opennlp.tools.tokenize.{TokenizerModel, TokenizerME, Tokenizer}
import opennlp.tools.postag.{POSModel, POSTaggerME}
import updown.util._

object TopicalChunker extends Logging {
  convertByte _

  var iterations = 1000
  var alpha = 30
  var beta = 0.1
  var numTopics = 3
  val fileSeparator = System.getProperty("file.separator")
  var childProcesses = List[Process]()


  val parser = new ArgotParser(this.getClass.getName, preUsage = Some("Updown"))
  val inputDocumentsOption = parser.option[String](List("i", "input"), "FILE", "training data for the model")
  val originalDocumentsOption = parser.option[String](List("o", "original"), "FILE", "the original data")

  val iterationOption = parser.option[Int](List("iterations"), "INT", "the number of iterations for the training the topicModel")
  val alphaOption = parser.option[Int](List("alpha"), "INT", "the symmetric alpha hyperparameter for LDA")
  val betaOption = parser.option[Double](List("beta"), "DOUBLE", "the symmetric beta hyperparameter for LDA")
  val numTopicsOption = parser.option[Int](List("numTopics"), "INT", "the number of topics for LDA")
  val saveModelOption = parser.option[String](List("save"), "FILE", "save the topic model to FILE")
  val loadModelOption = parser.option[String](List("load"), "FILE", "load the topic model from FILE")

  val outputOption = parser.option[String](List("o", "output"), "DIR", "the directory to dump topics into")
  val wordleOption = parser.flag[Boolean](List("w", "wordle"), "generate wordles for the topics (requires -o DIR) " +
    "(requires that you have downloaded IBM's word cloud generator)")
  val wordleJarOption = parser.option[String](List("wordleJar"), "PATH", ("the path to IBM's word cloud generator " +
    "(default %s)").format(WordleUtils.defaultJarPath))
  val wordleConfigOption = parser.option[String](List("wordleConfig"), "PATH", ("the path to the config file for IBM's " +
    "word cloud generator (default %s)").format(WordleUtils.defaultConfigurationPath))

  val sdetector = new SentenceDetectorME(new SentenceModel(new FileInputStream(new File("/data/chunker/en-sent.bin"))))
  val tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(new File("/data/chunker/en-token.bin"))))
  val posTagger = new POSTaggerME(new POSModel(new FileInputStream(new File("/data/chunker/en-pos-maxent.bin"))))
  val chunker = new ChunkerME(new ChunkerModel(new FileInputStream(new File("/data/chunker/en-chunker.bin"))))


  def doOutput(model: TopicModel) {
    if (outputOption.value.isDefined) {
      val file = new File(outputOption.value.get + fileSeparator + "run")
      file.mkdirs()
      val outputDirForThisRun = file.getAbsolutePath
      val summary = new BufferedWriter((new FileWriter((outputDirForThisRun + fileSeparator + "summary"))))
      summary.write("%s\n".format(model.getTopicPriors.zipWithIndex.map {
        case (a, b) => "Topic %s:%6.3f".format(b, a)
      }.mkString("\n")))
      summary.write("%s\n".format(model.getLabelsToTopicDist.toList.map {
        case (a, b) => "Label %9s:%s".format(SentimentLabel.toEnglishName(a), b.map {
          "%7.3f".format(_)
        }.mkString(""))
      }.mkString("\n")))
      summary.close()
      val outputFiles =
        (for ((topic, i) <- model.getTopics.zipWithIndex) yield {
          val outFile = new File(outputDirForThisRun + fileSeparator + "topic" + i)
          val output = new BufferedWriter(new FileWriter(outFile))
          output.write("%s\n".format(topic.distribution.toList.sortBy((pair) => (1 - pair._2)).map {
            case (a, b) => "%s\t%s".format(a, b)
          }.mkString("\n")))
          output.close()
          outFile.getAbsolutePath
        })
      if (wordleOption.value.isDefined) {
        logger.debug("making wordles and report")
        val index = new BufferedWriter((new FileWriter((outputDirForThisRun + fileSeparator + "index.html"))))
        index.write("<head><style>\n%s\n</style></head>\n".format(List(
          "div.bordered{border-style: solid none none none; padding: 5px; border-width: 1px; border-color: gray;}",
          "div#wordles{display:block; clear:both; padding-top:20px;}",
          "div.wordle{float:left;width:45%;border-style:solid; border-width:1px; border-color:gray; margin:2px;}",
          "div.wordle img{width: 100%;}",
          ".table{display:block; clear: both;}",
          ".row{display:block;clear:both;}",
          ".cell{display:block;float:left;}",
          ".values{display:block;float:left;width:300px;}",
          ".value{display:block;float:left;width:60px;}",
          "div.topicFreq .title{width:100px;}",
          "div.labelDistribution .title{width:150px;}"
        ).mkString("\n")))
        index.write("<body>")
        index.write("<div id=topicDistribution class=\"bordered table\">%s</div>\n".format(model.getTopicPriors.zipWithIndex.map {
          case (a, b) => "<div class=\"topicFreq row\"><span class=\"title cell\">Topic %s</span><span class=\"value cell\">%6.3f</span></div>".format(b, a)
        }.mkString("\n")))
        index.write(("<div id=labelDistributions class=\"bordered table\">" +
          "<div class=\"labelDistribution row\"><span class=\"title cell\">topic</span><span class=\"values cell\"><span class=\"value\">  0</span><span class=\"value\">  1</span><span class=\"value\">  2</span></span></div>" +
          "%s</div>\n").format(model.getLabelsToTopicDist.toList.sortBy({
          case (a, b) => SentimentLabel.ordinality(a)
        }).map {
          case (a, b) => "<div class=\"labelDistribution row\"><span class=\"title cell\">Label %9s</span><span class=\"values cell\">%s</span></div>".format(SentimentLabel.toEnglishName(a), b.map {
            "<span class=value>%7.3f</span>".format(_)
          }.mkString(""))
        }.mkString("\n")))
        val jarPath = if (wordleJarOption.value.isDefined) wordleJarOption.value.get else WordleUtils.defaultJarPath
        val configPath = if (wordleConfigOption.value.isDefined) wordleConfigOption.value.get else WordleUtils.defaultConfigurationPath
        index.write("<div id=wordles class=bordered>")
        childProcesses = childProcesses ::: WordleUtils.makeWordles(jarPath, configPath, outputFiles, Some(index))
        index.write("</div></body>")
        index.close()
        logger.debug("done making report and initializing wordles")
      }
    }
  }


  def main(args: Array[String]) {
    try {
      parser.parse(args)
      if (iterationOption.value.isDefined) {
        iterations = iterationOption.value.get
      }
      if (alphaOption.value.isDefined) {
        alpha = alphaOption.value.get
      }
      if (betaOption.value.isDefined) {
        beta = betaOption.value.get
      }
      if (numTopicsOption.value.isDefined) {
        numTopics = numTopicsOption.value.get
      }
      // Thanks to a bug in Mallet, we have to cap alphaSum
      val alphaSum = 300 min (alpha * numTopics)



      val inputDocumentsFileName =
        inputDocumentsOption.value match {
          case Some(filename) => filename
          case None => parser.usage("You must specify a gold labeled training file via -i.")
        }

      val inputDocuments = TweetFeatureReader(inputDocumentsFileName)

      logger.debug("alphaSum: " + alphaSum)
      val model: TopicModel =
        loadModelOption.value match {
          case Some(filename) =>
            new LDATopicModelFromFile(filename)
          case None =>
            new LDATopicModel(inputDocuments, numTopics, iterations, alphaSum, beta)
        }
      logger.debug("topic distribution:\n     :" + model.getTopicPriors)

      logger.debug({
        val topics = model.getTopics
        "topic distributions\n" +
          (for (i <- 0 until numTopics) yield "%5s: Topic(%s,%s)".format(i, topics(i).prior, topics(i).distribution.toList.sortBy((pair) => (1 - pair._2)).take(10))).mkString("\n")
      })
      doOutput(model)

      val document1 = "UGK has always came out with hits, but mainstream hip hop never gave them their respect. It wasn't until 'Big Pimpin' with Jay-Z that mainstream had ever heard of UGK, but now with this hot album, mainstream has no reason not give UGK the respect their deserveThe best yet! They came correct as usual! Pimp C Forever!Everything you would want in a mobile notebook. Battery life is the only shortfall. Would be nice if the wireless card port was located more towards the rear of the machine. As the card's antenna is extended, it becomes somewhat of an obstacle when using the keyboard. This somewhat-pricey backpack is an awesome buy. This thing holds so much stuff it's incredible! In addition to my laptop it has a huge storage section which easily holds an Xbox 360 with room to spare. Side pockets hold a water bottle on each side. Under that, pockets hold your laptop plug and and cables you may need. Front section holds any writing utencils, plus cell phone, charger, keys, etc. Also has a pouch for an iPod with a small hole to put your headphone cord through. \\n\\nPROS: So much storage space! Shoulder straps stretch and flex so your load doesn't hurt your shoulders. Padded back support and cushioned shoulder straps. \\n\\nCONS: Price. I wouldn't have bought it without a gift card I had. Also the chest straps can't be moved up or down, only tightened. \\n\\nOVERALL: Great bag for young road warriors who want something more stylish and rugged than a briefcase. She-ra was my favorite thing when I was little, and now it is my 4-year old daughter's favorite as well. She never tires of the adventures of the Great Rebellion. She runs around the house pretending to be She-ra, and plays with my old action figures while she watches the shows. She-ra is a great role model for young girls, not because she is strong but because she uses her smarts, never gives up, and always helps those in need, even if they are sometimes the enemy. Bottom line: Buy all of the Jazz Icons titles. Starting with the Brubeck, this series is the pinnacle of jazz on DVD. The biggest stars in their prime, all of them beautifully shot and with gorgeous sound. Anyone remotely interested in American music needs to have the whole series, starting with the Brubeck, though the boxed sets are the best value overall. Volume 2 of the first season has more great She-ra adventures to enjoy. It has some good appearances by supporting characters my daughter loves like Frosta and Castaspella. We never tire of watching these DVD's and can't wait for Season 2 to come out. this game system is raw any and everybody ashould get this oneThis TV is darn near perfect. The picture is as crisp as I could ever hope for, or imagine possible. The blacks are dark and rich, and the colors are vivid and natural. I watched \"300\" thru my blu-ray on this TV and I saw EVERY detail possible. I then watched \"Planet Earth\" thru the blu-ray - and I never even heard a word of narration the movie beacuse I was transfixed on the visuals. The only draw back is the lack of image-retention technology. This however can be easily remedied by breaking in the TV with repeated viewings of non-static images. I put a chapter of \"Planet Earth\" on repeat for 8 hours a day for a week. That did the job perfectly. If you want the best possible picture, the best possible resolution, and the best possible reliability, this Panasonic (in all its forms 42\", 50\", 58\") is as perfect a TV as money can buy. So go get one. When I first took the camera out of box, it's sleek style and beauty was just as amazing as the pictures it takes. This compact camera is jam-packed with so many features. This is the ultimate picture taking machine! Don't believe me? Try it for yourself!!I love mario and agian he strikes with another great game. I am not sure show I last this long without my Tivo Series3! I had a stand alone Series2 on my HD tube TV upstairs that got me started with Tivo. It changed my TV viewing habits (and my life) but I was constantly frustrated with the standard definition signal. When I finished my basement home theater I went with my Comcast HD-DVR... the Comcast Motorola box was like going back into the dark ages with a poor user interface, painful to navigate menus, and of course no \"Photos, Music, and More. \" I went through 2 Comcast boxes because they ran so hot in my cabinet that I had to replace them. I am now going on 1 year with my Series3 and LOVE it... using 2 CableCARDS and my HD off-air tuner I get all of the local HD channels plus my Comcast HD channels with Tivo's great user interface. With Tivo's 2.5 desktop software I am now able to stream my music and photos from my Vista PC to my HD TV. The only thing I am still waiting for is the software upgrade that will allow me to pull TV content off of my Series3 to PC. Have I mentioned that... I love my Tivo Series3. This unit is 100% unbelievable! Toshiba could charge $100 more and it would still be a great deal. Yes, it \"only\" sports a 1080i interface, but for most of us, there is very little difference. I have never noticed any \"combing\" from the 1080i feed. Also, this is the best upconverting/upscaling DVD player I have ever used and I have tried plenty. The upscaling performance, itself is worth the price. My normal DVD's look about %80 as good as my HD ones. Havent Purchased yet but have seen the amazing movie so far lowest price is at best buy but the story is INCREDIBLE even though it is not rated the worst thing in there is a bloody face because a boy got abused and a few swear words but nothing major it is a movie that u are sure to fall in love with and i would definally want to see it again and again,in fact i do!This is the best purchase I have ever made, and blows all my other systems away. Faaaassssst performance, reliable and a great deal. If you have the money, make the purchase. If not...borrow it!this game is great it's just like all the others from ps2 some new wepones and soume old one with different names. ratchet still kicks but and still hase cool guns. This player is excellent. Because it says \"plays for sure,\" you know it will work on Windows OK, but I don't use Windows, I use Ubuntu. This player worked perfectly. It played my music, wither in MP3, WMA or OGG format, which gives it a major advantage over most players. I was also pleased to find that it plays videos recorded in a standard, MP4 XVid format, meaning its easy to copy videos to your player. \\n\\nReviewers on the Internet rave about the battery life - they are all right. I've played and played this thing and the battery meter hasn't dropped a bit. I plug it into my computer once a week or so to copy music so that must be all it takes to keep the battery topped off. \\n\\nThe Radio feature uses the headphone cord as an antenna. I've not tried different headphones, so I don't know if switching to a new set will impair that feature. It pulls in FM signal crisp and clear though. \\n\\nFinally, my last favorite feature - two headphone jacks, so you can listen with a friend. :-)Decent computer for the money. It will run most high speed games but the processor isnt all its cracked up to be. I would definiantly try to find an intel based laptop if i could redo this. The graphics are also a let down, it isnt much better than my friends normal integrated and that doesnt mooch off of local ram. However, despite these shortcomings, its a heck of a computer for the 400$ I paid during a sell. I suggest upgrading both ram and processor since vista is made for dual core and 2 gigs of ram. Go with ram first if you dont have cash for both. This product replaces my old Linksys Vonage router that was taken out by lightening. Its very simple and easy to set up if you follow the instructions, I had phone service again within minutes. A big plus is the small size of this adapter, fits just about anywhere you want to put it. I have seen this roast at least 12 times on tv and all it did was get funnier. I purchased this system and it was def worth every penny... If you don't feel like building your own box and want everything in one package, this is the way to go. I had been looking at adding another monitor to my computer, I had a 17\" and two 15\"s. I was looking at adding another 17 to match but when I saw the price on this, I couldn't help it. After I unpacked and hooked it up, I went online to get another. Unfortunately they were out by then. Beautiful monitor, amazing color, awesome brightness! Whenever they get these back in stock I would buy ten of them for $175!!!My iPod made me by this. I had heard from some that macs were better than a pc. When my HP desktop died I decided to give Mac's a try (since they will now run XP). I figured anyone who could design and manufacture the iPod had to know something.. right? Picked this up, took it home and have not been able to stop saying \"wow\". \\n*Small footprint. Not much bigger than a 20\" LCD\\n*Elegant design\\n*Runs XP better than my HP box did\\n-Software selection is smaller but what is available is very good\\n-Some of the installed software seems simplier than Microsofts products\\n-Slight learning curve with 1 button mouse which I have now fixed with a microsoft bluetooth mouse\\nAll in all I was impressed with my new computer. I purchased \"Paralells\" that lets me run XP in a window. I also downloaded \"Boot Camp\" which allows me to run XP or even Vista from start up.. just like it was made for it... amazing!\\nAll in all I would give it an 9... the only downside is the higher cost of the computer to a PC. I got this headphones for my creative zen vision w, to start with i didnt like the headphones which came in with the player, So i bought an creative in ear ear plugs. First it sounded great then after 2 months my ear started to feel the pain. \\n\\nThen i decided to go far the Bose On the Ear headphones eventhough it was $170+. It sounds great with the player, i watch back to back movies in my player and still it doesnot hurt, The Soft leather which is in contact with the ear is very good, Sometimes my ear tend to get little warm but little warm for back to back movies should be OK i guess.\\n\\nI still think it is a bit overpriced eventhough its BOSE!!!I'm not a huge movie buff, so I can't tell you if this is 'Leo's' best or not... but I will say this. It's about the only movie released in the last year I'd be tempted to buy and take home! Suspenseful throughout, a good ending, and I thought all the actors did a good job (but I usually don't pick winners when the awards come around... just a fair warning). I thought it'd be good with the names it carries, but it by far surpassed expectation. Genre-wise, I'd put it something between Training Day and 16 Blocks. Great movie, I highly recommend it. This card is well worth the price. This card is where the gaming industry had always intended gaming to be and in the near future this will be the standard. Things don’t just look real; they act real and feel real. Massively destructible buildings and landscapes; explosions that cause collateral damage; lifelike characters with spectacular new weapons; realistic smoke, fog and oozing fluids are all now possible with the AGEIA PhysX processor! You truly feel like you're in the game. Example shooting a concrete wall you see the small pieces of debris fly off and the details are crisp. Another is when pieces of debris flys off and it come in contact with some other physical object it will react as it would in real life. The lush foliage that sways naturally when brushed against by you or others. This game is only available certain games now... but others will come... \\n\"That's What I've Been Saying!\"This is a great series. I nice new look on the classic tale of Robin Hood. The acting is great, especially that of Keith Allen. It can sometimes be a pretty corny show, but if you don't let that bother you it is a very enjoyable show. It is clean, funny, and action packed. A truly creative look on the story of Robin Hood. It is a must see for all true Robin Hood fans. The title says it all! This is truly the best and most affordable way to get into next-gen DVD's!\\nI bought this machine simply cause of the price, the urge to get into the next-gen DVD format, and for a pretty decent collection of movies. So far I have Shooter and King Kong and both look OUTSTANDING on my HDTV on 720p. I ordered 300 and GoodFellas to also add to my collection since those are two of my favorite movies! Yes I know that 1080p is the way to go, but 720p still looks phenominal, especially being compared to regular ol' DVDs. \\nAlso the menus are quite nice, in that you can access the main menu anytime when watching the movie!! Some HD-DVD movies also have an ability to view special features, such as: picture-in-picture while watching the movie!! 300 is supposed to have some of the best special features on an HD-DVD!!\\nOverall, if you own an HDTV whether it's 720p or 1080p, BUY THIS NOW!!!! It is seriously the most affordable way to enjoy your favorite movies in HD. And if (and most likely) Blu-Ray wins the format war, don't fret! At least you didn't spend $500 - $1000 dollars when HD-DVD players first came out. So what do you got to lose? Buy this machine now!!!Wildly entertaining! Laughed all the way through the movie. The four independently styled actors meld perfectly to keep the excitement high and the comedy non-stop. I purchased this from best buy about 2 weeks ago. \\nI have seen three blu-ray movies on it. I think this is the most incredible picture I've ever seen. \\nI love this TV. I find this album very very good,i have all the other ws albums and they are all good in their own way. This album has the distinct sound of the white stripes. Its kind of like a garage band mixed with a lot of funk. Whether its the driving beating drums of Meg White or the wailing voice and guitar of Jack White its an all around fantastic album!I was fortunate enough to be invited to a private listening party to hear tracks from the album on 8/1/2008. After hearing Bitter for Sweet, Stiff Kittens and Semiotic Love on their Myspace page, the 3 additional tracks were even futher proof that these two gentleman are on to something! We were given the album two weeks early and I have been listening to it every day since then. The album is driven by Jades songwritting talents that match and compliment Davey's lyrical abilities so well. The subject matter seems to be a bit more personal than previous work released by these two musicians from AFI. The songs that stand out for me are Semiotic Love which strikes me as an emotional discharge in a very nice contrast to the semi uplifting beat/music; The Fear of Being found seems to be directly influenced by the works of the wonderful Depeche Mode and has that same meloncholy approach to describing feelings that most of us have yet are unable to completely speak freely about; Again, Again and Again presents a struggle in controlling the urge to dance, a great song; Bitter for Sweet is probably the closest thing on the album to a song from AFI is that is what you are looking for in this album. However, be warned that you are barking up the wrong tree if you want an continuation of DECEMBERUNDERGROUND. This is a new expression of musical abilties, keep your preconceived notions away and be ready to dance! I believe the Best Buy version has an additional song exclusive to them, so pick this version up along with the different versions released to other retailers and iTunes. You wont be sorry!This is a great product. It fits my laptop perfectly. I have a Del Inspirion 1501, it's a little snug but that is how i wanted it. It's perfect if you plan to place your laptop in your bag and carry it. I prevents it from getting scratched from carelessness. Awesome product, and it's made by Targus, they've been making products for a while. realyy great movie, its funny, action packed and very suspenseful:)))This is the best game to come around in a long, long time. I recommend this game to anybody. This is my third pair of noise canceling equipment. In this category, you get what you pay for. $200 is a hefty price for headphones but Sony delivers. Other pairs create a nasty white noise when the cancellation is active. While a very quiet noise is still produced with these, it is far better than cheaper brands and is not noticeable when music is playing (tones sound brilliant with great highs and a surprisingly nice bass) or a fair amount of background noise is present. I wore these on the ride home from best buy and I felt like I was driving inside my car's stereo because that is all i could hear! There is even a button that mutes what you are listening to so you can hear the outside world whenever you need to without taking them off. the legends of rock guitar hero has the best list of master tracks out of all 4 games (Xbox 360 included) i have tried a demo of this game and it will blow you away!!! peace>My December is not like breakaway and certainly not like thankful. its a raw sound that just begins to touch on what Kelly Clarkson can really do. Kelly puts all her emotions into these songs and you can feel, and hear it. its brilliant and she is brilliant!.. give it a chance.. cause you wont be disappointed!!This guitar looks nice and works well. It seems a bit heavier than the hammer or other wireless models. The range is more than needed with no issues. There is even a blue LED light that illuminates by the strum bar. The only downfall with the one I purchased was it was harder to get it to go into Star Power Mode. When you point the guitar up you had to give it a little shake as well unlike other models. I happened to walk in the store the day after they received this computer and based on the size and the price I was hooked. I'm a pilot and portability was a must, as my last notebook was a 9 pound monster. The size of the 13.3\" screen is just right for someone who's looking to mainly surf the internet and read email. \\nAt first use I found the computer to be somewhat pokey, mainly during the start-up and shut down cycle. Ultimately, I found that a piece of Toshiba software, Toshiba Flash Cards, was the culprit and removed it from the system. The computer now runs smoothly and efficiently. \\nBattery life on this system is far better than other laptops I've used in the past. I get nearly 4 hours on the battery, if I can refrain from using the optical drive. \\nThe form factor and layout of the computer is good also, it's just large enough to be comfortable to use. If you've been thinking about getting a 12.1\" notebook, but find it just a little too small to be comfortable, the 13.3\" size is a perfect compromise. \\nOverall, I'm very satisfied. I'd recommend this computer to anyone who needs portability with basic computing needs. This whole series has been great and I believe that anyone who purchases this series will feel the same and those who watch a episode or two will definitly want to own the series, Because its just that good!!!Great extras make rewatching and re-rewatching the film very enjoyable!\\nThe movie is well crafted with great direction. Kudos to the Brits for showing the US how to really do a \"buddy cop\" flick! The film is well paced with a reasonable amount of action, a great twist, excellent comic timing, and plenty of intra- and extra-genre references for the movie buff. great ps3 controller for any who is a big eagles fan like myself great purchase in time for madden 08 great to play with madden 08 go get one nowworks great,very easy setup under 5 min. just pop in the disk and it gives you step by step directions. Its a good anime to watch and when I bought it, it came in good condition. A good anime to buy and watch it never gets boring at all. If you like vampires with the living died in animation you will definitely like this one, I did. If you like robots with funny characters that pilot them you like this anime. This is one of the best and funniest movies I've ever seen BUY IT!!!!!!!!!Right out of the box."

      val document = "This is one of the best and funniest movies I've ever seen BUY IT!!!!!!!!!  Right out of the box, this 40\" LCD reaches out and begs you to plug her in! I hooked it up easily and had a picture to make my wife proud almost immediately. The presets were already awesome and we were awestruck! The colors and clarity are sensational and trump those of the Sony Trinitron Wega we are replacing.\\nThe cosmetics of this set are elegant, with the black piano frame and hidden from notice front speakers.\\nThere is no noticable glare on this screen that I can detect and we have it in a large vaulted family area with lighting and windows galore. I chose the LCD over a Plasma for that reason.\\nI am delighted with the price and sales transaction from BestBuy and look forward to having them visit in a few days to fine tune the big screen. This trip may not even be necessary but I am sure I will learn more about this hightly sophisticated jewel of a Big Screen LCD HDML TV.\\nI think anyone would be pleased with this new Sony Model KDL-40V3000 and the its many features, which are listed in the specifications.  I bought recently a 42' 720p Panasonic Plasma Television from Best Buy East Washington Street. I recomment this store as well as this brand of television if not the Pioneer Plasma Televisions. Consumer Reports rate them at the top and Im seeing it every night on my new blue ray player, dont miss a chance to upgrade to the new technology in this television let alone the new blue ray technology. Blockbuster recently acquired the rights to carry blu ray movies in their stores and I believe this is the way to go.  Best digital SLR camera on the market. Can hold its own even when compared to its standard 35 mil. counterpart.  extremely happy with this buy, highly recommended. just bought the cumpter as a replacement for my old one. very good buy for the money. A++++++ buy  Finally, a dual zone receiver under $1000! With Yamaha's THD at such a low number, this receiver is the most efficient amplifier currently sold by Best Buy. I choose Yamaha for there reputation and sound quality. Also, I wanted dual zone on a budget with HDMI.  Easy to use,and very confortable no power cord need it,very reliable and no false alarms or warnings.  You need to buy this . . . that's about it. But if you want a little more this is a TV show from the early 80's that was honestly one of the greatest kids shows in the history of time. The use of Muppet technology in this show was ground breaking and should be watched by everyone. They live in a cave, sweet!  It's so beautiful yet so powerful. Paul Gilbert has been a huge influence on me a a guitarist. They don't get the credit they deserve.  Adrien Brody is not only hot, yet very talented. No wonder why he got an Oscar. I love this movie. If you love Adrien Brody then you will love this film.  Pro-I am very happy with the performance of this router. This router supports WDS (wireless distributuion system), which allows you to extend the wireless network. I purchased two routers, one used as a router, and the other (located in another building accross the street) is switched to be used as a access point. I now have very good wired and wiress internet access in both buildings. Buffalo was the only one that has a switch on the bottom of the router allowing you to switch it to a access (or bridge) point. Cons- I needed Buffalo support for help with the settings. (note WDS not avaliable with their Draft N products)  I KNOW NOTHING AT ALL ABOUT THESE LITTLE MP3 PLAYERS BUT THIS ONE WAS A GREAT CHOICE! IT WAS EASY TO FIGURE OUT AND COMES LOADED WITH A LOT OF FEATURES! I LOVE HOW EASY THE SOFTWARE IS TO MANEUVOR AND THE PICTURE QUALITY IS FANTATSIC! THE BATTERY HOLDS A CHARGE FOREVER!!! AND ...IT'S SO TINY AND CUTE!  Kissology is the best dvd put out by the band. Us kiss fans have been waited a long time for this dvd!!!!!!!!!!!!  This is the most soulful album I've heard in years. I love this album. You have to get it, once you hear it I know you'll tell a friend. I couldn't keep this to myself! I don't endorse artists often, but this man is the bomb. See him and his band live, wow!\\n\\nI never knew he made my favorite house song \"Don't Change for Me.\"  I would highly recommend this unit. I read MANY reviews about this unit and a lot of them said the built in fm transmitter did not work well at all. My experience has been the exact opposite. I live in the Minneapolis/St. Paul metropolitan area where there are lots of radio stations and I have not had any problems with the fm transmitter that is built into this unit. I also use the included \"sure connect\" that comes with it. I have found it to work extremely well and the sound quality is great! I am very pleased with this unit and would recommend it to anyone.  I have had this DVR since the day it became available last year. At first it had alot of issues, but DirecTV has made alot of upgrades to the software and now I do not hesitate to recommend this unit. You can set the unit to record every episode of a season, I now have it hooked to my home network and can view pictures and listen to .mp3s, and the picture is great!\\nOnce we buy a 2nd LCD for the bedroom, we'll be getting another HR20!  I have been looking for a new HDTV for about 1 year. After reviews and going to BB like every day I picked this TV.\\nPicture Quality is Second to none. It made the Sony SXRD look bad. This TV is also 3D ready. This Fall Samsung and their partners are to release Software (firmware update) and glasses that will turn this into 3D home TV. The 10,000 to one cantrast ration makes the blacks look deep. I have an xbox360 and the 1080p through component looks great. My fios TV through HDMI looks wonderful also. I'm waiting on either Blu-ray or HD-DVD to win the format war so I can get a HD-movie player.\\nI got this for $1700 and I would not trade it for any TV. If you are a gamer and a movie watcher you gotta get this tv. You have not experienced HD right if you don't have the right TV.  I have had various PCs in my home for over 25 years and this has got to be one of my favorites. Having moved to a small apartment in order to get my PhD, this computer is perfect...one wire to plug it in, TV and computer all in one, a great screen, compact, and easy to operate. I was planning on spending almost $4,000 to build a PC online to get everything I wanted, and this saved me a ton of money and gave me almost everything I would have included. The light-scribe DVD was just a fun bonus. I may decide to add my surround speakers later, but for now, the sound is even better for apartment living. It's one of the Best Buys I've ever made. Thanks!  I've had this sweeeet player (30 gig model, black) for over a year now and haven't had a single problem with it. Where to start???\\nThe Zen's screen is beautiful for anyone who enjoys watching videos...as it is approx. 256000 colors, compared to ipod's 65000 color screen (4x greater) and the details are pretty much crystal clear. Plus, you even have the option of adjusting the brightness of the screen (anywhere from 10% to 100%) which is a feature not included w/Ipod. U can choose ur own background pic as well to give it a unique look. the screen, however, can catch fingerprints easily but i dont have a problem since i use a cloth to clean it off every now and then.\\nBattery life is pretty long, 14 hours audio or 4 hours on video, which is way more than ipod's battery.\\nThe ZEN takes the win totally when it comes to features. It has a built in FM radio w/recorder, a microphone, and can function as a removable disk.\\nAs far as size goes, it is bigger than the Ipod, but I like it cause it feels firm in my hand. I dropped it maybe once or twice unfortunately, but it works fine. Scratches are very minimal, pretty much unnoticable, even though i have black color! u have to look pretty hard to see them.\\nFinally, sound is amazing with these headphones that are generally overlooked!!! The player also comes with an awesome equalizer and bass settings that can make it sound just right.\\nAlso, the touchpad might be annoying to some initially as it took me about 10 min getting used to it. However, u can adjust its sensitivity and then it becomes fine.\\n\\nPROS: screen quality! battery life! audio! TON OF MORE FEATURES THAN IPOD!!!\\nCONS: software not the best, player prone to fingerprints, some may find it bulky...comes w/USB charger, not a power adapter\\n\\nThis is an excellent player for the price (u can find better deals if u look around) and is officially the ipod killa!!!  Two Words.....Ah-Mazing\\nI loved it to the fullest\\nshe is the funniest person alive!!!  I was up late one night and fell asleep with the television on. I woke up to a beautiful song. Brandi Carlile, who is that? I was in a trance at her voice and songs. I just cant put into words how calm her songs made me. A true talent and gift to this world! I recommend to every one.  I would like to let people know that if you are looking for a nice ipod that plays videos this is the one to go with. I have over 10,000 songs and still have room for more like videos and photos. The photos come out clear and I love it!!!!! 80gigs might be a bit much but the 30 is just right. I still haven't filled it up!!!! Must buy!!  I got this for my boyfriend. It was actually for hunting season for the slow days but he uses it everyday .We looked at several brands. He compared everything and in our opinion this is by far the BEST value / memory / rechargable battery / size screen / great price . We know people that have the Zune and other players and the drawbacks and we are more than pleased with the Insignia. In fact im getting 1 for myself soon. Thank you Insignia !  I took this unit on a trip to Niagra Falls, Ontario, from southwestern, PA. It was unbelievable!!! The initial set-up had very little instructions on how to set it up, such as where the plugs (power) go, but it did not take long to figure it out. The voice was very clear, the screen was easy to view. I used it when we were in Canada, and all of the info was just as accurate. If you miss a turn even to pull in and get gas it recalculates and gets you back on track within seconds. It did route us to a bridge that was used only for the Customs agents, but it has a Detour button and when you push it...Recalculating past it, and we are moving again. The unit also keeps an estimated time of arrival on the screen so you can answer that question...When are we gonna be there??? Make sure you get the one that has the text - voice (this one does have it), it really helps when there are multiple streets to turn on. It has it's flaws when you first start using it, but once you figure out the many options it is very easy to use. Not a \"Best Buy\" but a Great Buy for the money!!  Hot very hot, the graphics are crazy, the controls are great and the challenge well lets say i loved to hate the game, but when i did pass a hard track i felt like a king.\\ngreat game.  The feature I enjoy the most about this television besides its picture and size is the ability to choose to save additional electricity. When you set up the LCD you can choose to prevent the \"vampire\" effect. I will be purchasing the 32\" shortly, because I am so pleased with my 26\".  I live in a two story unit on the top two floors of an old building (think: \"heat rises\") with 12 foot ceilings. On Friday morning, my central air went on vacation, and after a few phone calls, I found out no one could come out to make repairs until Monday (at the earliest).\\n\\nSince I was facing 95+ degree weather for the weekend, I decided I needed to do SOMETHING. I bought two of these little window units, and put one in upstairs and one downstairs. The temperature in my condo was at that point 94 degrees.\\n\\nAfter two hours of operation, my condo was a comfortable 76 degrees. By Saturday, once the two A/C's had the chance to \"catch up,\" they had no problem cooling my home to a more than comfortable 70 degrees.\\n\\nI must also say that although each unit is rated to cool 150 SqFt, that is understated. The downstairs unit is cooling roughly 550 SqFt and the upstairs unit is cooling roughly 600 SqFt. They are working non-stop, but doing a great job of cooling my home.\\n\\nThe units are a tad on the noisy side, but not unreasonably so -- the noise has faded into the background, now that I've become used to it.\\n\\nI highly recommend this product, especially considering the price and the fact that two of them are cooling 1150 SqFt to 70 degrees (despite the fact that combined they are rated to cool only 300 SqFt).\\n\\nA GREAT buy, and a lifesaver on a 95 degree weekend without central air!!  this cd is awesome if you want to see and hear some of the great songs on it check out the mhs marching bands 2007-08 halftime show  This game is really cool. I love how you can interact with different charecters and how Nancy Drew protrays different people in different games. I have learned alot from Nancy Drew games.\\nBUY NANCY DREW!!  Having read about this movie on other sites, I decided, \"Hey, this film is worth a shot.\" I also enjoy period, fantasy, romance and adventure films. A friend of mine bought the film for me and I can't stop repeating the lines. \"As You Wish,\" Hello. My name is Inigo Montoya. You killed my father. Prepare to die,\" \"Inconcievable,\" etc. The performances are magnificent-from Cary Elwes (in his second movie) to Robin Wright (before she Married Sean Penn) and the rest of the cast. The movie manages to make fun of traditional fairy tales while not taking itself seriously. As Rob Reiner says in the all-new documentary \"As You Wish: The Making Of The Princess Bride,\" it's a celebration of true love. The sword fight on the Cliffs Of Insanity is terrific and remeniscent of Errol Flynn swashbucklers. Of course, there is romance...and beautiful love scenes they are. With the film itself being free of any objectionable material, you might almost think that this is a Disney film. Well, it's not, but Walt Disney himself would be proud of this movie. Romance, adventure and comedy all in one spectacular movie. It's a chick flick. Recommended for girls' night out. Rated PG for mild adventure violence.  Spacious fridge and one of the few of its kind we could find with water on the door.  this game is aswem it letes u create ur own person and everything its by far the best game for the wii.the wii is the bestsystem and this game reassures that so if u want a very in tertaing game buy it!!PEACE OUT!!!!!!!!!!!!!  This bag not only accomodates some of the largest notebooks around, but will also seemelessly adjust to smaller models. The design takes into consideration all of your other needs; from storage of accessories, and other devices like your P.D.A., cell-phone, and music player. You can even keep your music player safely inside, and run your headphones through the simple but ingenius slot made for just that purpose. The padding adds a layer of comfort and security you seldom find in any case on the market. There is just not enough to be said about this bag. If you need to store, carry, or show off your notebook, or just want to know it's safe, this is the product to buy.  strategic rationality. spontaneous revelation. superb reluctance. Dont miss it.  It is a very nice product!And I like it very very much, thanks apple!!!  if you like hardcore metal then you will love children of bodom! they have a very unique sound that you wont find anywhere else. in my opinion the best song on this c.d is \"triple corpse hammerblow\" but the whole c.d is really worth buying. so buy this c.d! support the hardcore!!!  this MAC is amazing. More fun and better options have been added.  I have two of these printers. Worth every dime. The only problem I ever had with either is the pick up tray in the back pulling the next sheet in. And that was only after I had them for over a year (the rollers were probably needing service or replaced). Very good quality print, very easy to operate, very easy to scan and the scan quality is superb. Overall, I'd still give this printer a 5 star rating. I use these for business so they do get used quite a bit. Thanks, Canon!  Plays great\\nGreat gaming expierience\\nXbox Live rocks  They \"snacked on danger & Dined on death\" for 2 decades, dominating professional wrestling and Imortalizing themselves as the only tag team ever to\\nhold all 3 major titles (NWA(WCW), AWA, WWE(WWF)\\n\\nHawk and Animal, Better known as The Legion Of Doom,\\nrampaged thru the AWA, NWA, All Japan Pro Wrestling,\\nWCW, & WWE.\\n\\nIn the DVD's The stunning journey unfolds, from their auspicious debut in the early 80's to the tragic death of Michael \"Hawk\" Hegstrand in 2003. You'll ride along with\\nthe Road Warriors' most memorable bouts interviews and more.\\n\\nBrutal. Commanding. Unforgetable.\\n\\nOh, What a Rushhhhhhhhhhh!\\n\\nI have to say, that if your a major fan of this tag team then this is the DVD for you, both the DVD's have everything you expect from the LOD aka The Road Warriors, Disc one has interviews, clips and background of the most feared tag team in wrestling history, Disc 2 has 14 of LOD's best matches. This set of dvd's comes Highly recommended for you LOD fans.  My husband and I purchased this monitor with a complete package deal at our local Best Buy store and we love it! It is like watching tv, but typing. I never thought messing around on a computer could be this fun until we got this huge screen. It is a 22 inch Westinghouse monitor that goes great with our CP unit and internet capabilities. We are impressed by it so much and so is everyone that has seen it. Thank you Best Buy and hope others enjoy it as much as we have!  This movie is the best and its really funny.The movie makes fun of movies like scream,I know what you did last summer and a bunch of otheres.Its way better then scary movie.you should go buy this movie caus it is worth to buy and you will be laughing threw the whole thing I promise you that.  At first, you are not sure why you are watching this movie. Then, you are drawn into a charming. funny story remenicent of the best of the office, scrubs and your favorite romantic comedy. This is a chick flick for guys and a buddy picture for chicks. By the end I was grinning from ear to ear with tears in my eyes. A must see and one you will want to own and watch again and again.  this a great movie. the fight scene between leo and raph was great. this is one of the best tmnt movies i have seen  The P520 is a nice little recorder to capture the special moments that matter.\\nStill trying to get an update for the software!\\nAny links for windows 2000nt  By far the best new cd I have heard in awhile. His music combines a super sweet Jazz feel with a freestyling lyrical genius in only away Jake Smith can. And the best part is that he is even better live...This CD is far from boring and will have you nodding your head to the beat track after track. If you are looking for a new artist that truly encapsulates soul and R&B while maintaining a new fresh feel Jake Smith's Real is the REAL deal!!! So amazing.  I purchased this Toshiba HD DVD player after carefully considering the HD vs BLU-RAY format war. With no immediate end in sight, I decided to take my chances with HD DVD and I was not disappointed. The unit is easy to install and to operate and on my Panasonic 32 inch LCD TV with 720p resolution, the images in hi def are worth the investment. Especially impressive is the PLANET EARTH series, which was filmed in hi definition. What I learned from my initial excitement in buying different HD DVDs, is that not all HD DVDs look hi def, most likely from the compression process, the movie being older, etc. But recent movies, and of course, those filmed in hi definition, bring a beautiful, clear, crisp, video image. Audio reproduction is equally impressive. I would highly recommend this unit to anyone looking to jump into the world of hi definition. "

      // now, annotate a document.
      val instanceID = 0
      val instance =
        GoldLabeledTweet(instanceID.toString, "",
          TokenizationPipes.filterOnStopset(Set())(
            TokenizationPipes.filterOnRegex("(\\p{Alpha}|')+")(
              TokenizationPipes.twokenize(
                TokenizationPipes.toLowercase(List(document))))),
          SentimentLabel.Positive)
      val topicDist = model.inferTopics(instance)


      val sents = sdetector.sentDetect(document).toList.map((s) => s.split("but|;").toList).flatten.filter((s) => s.split("\\s+").length >= 5)

      val results =
        (for (sent <- sents) yield {
          val tokens = tokenizer.tokenize(sent)
          val myTokens = TokenizationPipes.filterOnStopset(Set())(
            TokenizationPipes.filterOnRegex("(\\p{Alpha}|')+")(
              TokenizationPipes.twokenize(
                TokenizationPipes.toLowercase(List(sent)))))
          val topics = model.getTopics
          val scores =
            (for ((Topic(prior, distribution), index) <- topics.zipWithIndex) yield {
              val d2 = distribution.withDefault((s) => 0.0)
              prior * myTokens.map((token) => d2(token)).reduce(_ + _)
            }).toList
          val maxScore = scores.indexOf(scores.max)
          logger.debug(maxScore + "\t" + scores + "\t" + sent)
          maxScore
        }).groupBy((x) => x).map {
          case (k, v) => (k, v.length.toFloat / sents.length)
        }.toList.sorted

      logger.info(results.toString)
      logger.info(topicDist.toString)

      val chunks =
        (for (sent <- sents) yield {
          val tokens = tokenizer.tokenize(sent)

          val tags = posTagger.tag(tokens)
          val chunks = chunker.chunk(tokens, tags)
          chunks
          var chunked = List[List[String]]()
          var chunk = List[String]()
          for ((token, tag) <- tokens.toList zip chunks.toList) {
            val (mark) = tag.charAt(0)
            mark match {
              case 'B' =>
                chunked = chunk.reverse :: chunked
                chunk = List(token)
              case 'I' =>
                chunk = token :: chunk
              case 'O' => ()
              case x =>
                logger.error("Got unexpected token, tag output from the chunker: %s,%s".format(token, tag))
            }
          }
          (chunk.reverse :: chunked).reverse
        }).flatten.filter((c) => c.length >= 5)
      val results2 =
        (for (chunk <- chunks) yield {
          val sent = chunk.mkString(" ")
          val tokens = tokenizer.tokenize(sent)
          val myTokens = TokenizationPipes.filterOnStopset(Set())(
            TokenizationPipes.filterOnRegex("(\\p{Alpha}|')+")(
              TokenizationPipes.twokenize(
                TokenizationPipes.toLowercase(List(sent)))))
          val topics = model.getTopics
          val scores =
            (for ((Topic(prior, distribution), index) <- topics.zipWithIndex) yield {
              val d2 = distribution.withDefault((s) => 0.0)
              if (myTokens.length > 0) {
                prior * myTokens.map((token) => d2(token)).reduce(_ + _)
              } else {
                0.0
              }
            }).toList
          val maxScore = scores.indexOf(scores.max)
          logger.debug(maxScore + "\t" + scores + "\t" + sent)
          maxScore
        }).groupBy((x) => x).map {
          case (k, v) => (k, v.length.toFloat / chunks.length)
        }.toList.sorted

      logger.info(results2.toString)
      logger.info(topicDist.toString)

      if (saveModelOption.value.isDefined) {
        model.save(saveModelOption.value.get)
      }
    }


    catch {
      case e: ArgotUsageException => println(e.message); sys.exit(1)
    }
  }

}