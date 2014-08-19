# jcha - Java Class Histogram Analyser

jcha is a tool to analyse Java class histograms, focussed on histogram comparison.
It can aid in debugging memory leaks, or for finding dominators before issues arise.
Supported histogram formats are those from "jcmd pid GC.class_histogram".

It can run compare 2 or more histograms, and find correlations between classes.
You can either use the command line tool jcha  or the JavaFX GUI application jcha-gui.

Both tools support class filtering, by either giving a limit or a class list.


## Building
  mvn package

## Running
You can run jcha directly from the build directory
```
 jcha histogram1 histogram2
 jcha -h  # for showing help on all options like sorting
 
 jcha-gui histogram1 histogram2
```

Shortcut, using a shell alias:
```
 path=`pwd`
 alias jcha=$path/jcha
 alias jcha-gui=$path/jcha-gui
```

## Possible future enhancements
 * Correlate class statistics to find common characteristics like "growing about the same amount"
 * Auto-selector, that finds most interesting classes for jcha-gui (a graph with 100 classes does not make much sense)
 * Using the classpath of the analysed application, find the culprit class causing a leak.
 * Optionally use timestamps instead of numbers as x-Axis. Either from file inode (-t [ctime|utime|atime|time])
   or filename pattern (-t name regexp)

## License
Apache License 2.0
Copyright 2014-present trivago GmbH
