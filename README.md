# MMT 0.14-alpha - Release for Ubuntu 14.04 

## About MMT
MMT is a context-aware, incremental and distributed general purpose Machine Translation technology.

MMT is:
- Simple to use, fast to train, and easy to scale with respect to domains, data, and users.
- Trained by pooling all available domains/projects/customers data and translation memories in one folder.
- Queried by providing the sentence to be translated and some context text.

MMT's goal is to deliver the quality of multiple custom engines by adapting on the fly to the provided context.

You can find more information on: http://www.modernmt.eu

## About this Release

This release allows you to create an MT engine, from a collection of line aligned parallel data or TMX files, 
that can be queried via a REST API.

Intro video: http://87k.eu/lk9l

## Your first translation with MMT

### Installation

Read [INSTALL.md](INSTALL.md)

The distribution includes a small dataset (folder `examples/data/train`) to train and test translations from 
English to Italian in three domains. 

### Create an engine

```bash
$ ./mmt create en it examples/data/train
```

### Start the engine

```bash
$ ./mmt start
```
You can stop it with the command `stop`.

### Start translating

Let's now use the command-line tool `mmt` to query the engine with the sentence *hello world* and context *computer*:
```
$ ./mmt translate --context computer "hello world"

ModernMT Translate command line
>> Context: ibm 87%, europarl 13%

>> hello mondo
```
Next, we are going to improve the partial translation `hello mondo`.

*Note:* You can query MMT directly via REST API, to learn more on how to do it, visit the [Translate API](https://github.com/ModernMT/MMT/wiki/REST-API#translation-api) page in the project Wiki.


### Improve translation quality with new data

Let's now add a contribution to te existing engine, **without** need for retraining, in order to improve the previous translation. We will use again the command-line tool `mmt`:
```
./mmt add ibm "hello Mike!" "ciao Mike!"
```
And now repeat the previous translation query: the engine has just learned a new word and the result is immediately visible.
```
$ ./mmt translate --context computer "hello world"

ModernMT Translate command line
>> Context: ibm 87%, europarl 13%

>> ciao mondo
```

## Evaluating quality

How is your engine performing vs the commercial state-of-the-art technologies?

Should I use Google Translate or ModernMT given this data? 

Evaluate helps you answer these questions.

Before training, MMT has removed sentences corresponding to 1% of the training set (or up to 1200 lines at most).
During evaluate these sentences are used to compute the BLUE Score and Matecat Post-Editing Score against the MMT and Google Translate.

With your engine running, just type:
```
./mmt evaluate
```
The typical output will be
```
Testing on 980 sentences...

Matecat Post-Editing Score:
  MMT              : 75.10 (Winner)
  Google Translate : 73.40 | API Limit Exeeded | Connection Error

BLEU:
  MMT              : 37.50 (Winner)
  Google Translate : 36.10 | API Limit Exeeded | Connection Error

Translation Speed:
  MMT              :  1.75s per sentence
  Google Translate :  0.76s per sentence
  
```

If you want to test on a different test-set just type:
```
./mmt evaluate --path path/to/your/test-set
```

*Notes:* To run Evaluate you need internet connection for Google Translate API and the Matecat Post-Editing Score API.
MMT comes with a limited Google Translate API key. 

Matecat kindly provides unlimited-fair-usage, access to their API to MMT users.

You can select your Google Translate API Key by typing:
```
./mmt evaluate --gt-key YOUR_GOOGLE_TRANSLATE_API_KEY
```

If you don't want to use Google Translate just type a random key.

## Increasing the quality

### How to prepare your data

The easy way to increase the quality is to add more in-domain data.

MMT uses standard sentence aligned corpora, optionally divided into files by domain. 

Example:
```
data/microsoft.en
data/microsoft.fr
data/europarl.en
data/europarl.fr
data/wmt10.en
data/wmt10.fr
```

In general:
```
domain-id.(2 letters iso lang code|5 letters RFC3066)
```

Note: domain-id must be [a-zA-Z0-9] only, without spaces.

#### Get more parallel data

If you need more data there is a good collection here:

http://opus.lingfil.uu.se

#### Add monolingual data

The MMT language model is created with the target of the parallel data and extra monolingual data provided by the user.

To add monolingual data just add a LM-NAME.target_lang to the train folder.

Example:
```
data/my_monolingual_data.fr
data/microsoft.en
data/microsoft.fr
data/europarl.en
data/europarl.fr
data/wmt10.en
data/wmt10.fr
```

### Creating a large translation model

You can create a 1B words engine in around 4 hours of training using 16 Cores and 30GB of RAM.

If you want to try, you can download the [WMT 10 Corpus](http://www.statmt.org/wmt10/training-giga-fren.tar) corpus from here:

```
wget http://www.statmt.org/wmt10/training-giga-fren.tar
```

Untar the archive and place the unzipped giga-fren.release2.XX corpus in a training directory (eg. wmt-train-dir) and run:

```bash
./mmt create en fr wmt-train-dir

