# Insider

Insider is a belt of tools that, built on the idea of searching regular expressions in code. Insider is able to detect code smells, the usage of external libraries and the topics that a software project approaches by only using Regex. This means that Insider does not use any type of parsing and is therefore language independent.

## Installation

### From Github
To install Insider from Github, please download from the [latest release](https://github.com/dxworks/insider/releases) from Github, the `insider.zip` archive and unzip it to a specific location.
The contents of this archive are:
* `insider-*.jar` - the executable jar file
* `insider.bat` - a batch script for executing Insider on Windows
* `insider.sh` - a shell script for executing insider on Windows
* `config` - a folder for configuration files, described more in the [Configuration Section](#Configuration)
* `results` - a folder where Insider will output the results.

### From Docker
...

### From Code
Clone the repository from [here](https://github.com/dxworks/insider).

Run `gradle clean build` to obtain an executable jar.

## Configuration
Edit the configuration file (*config/insider-conf.properties*), to **specify the root folder** of the project's sources.

## Commands

### Find Command
* In order to find the occurrences of the libraries in the analyzed project. Use the following command (run Insider using the **insider.bat** or **insider.sh** script):
```
insider.sh find config/libraries.json
```

* Detect Simple Code Smells. Use the following command (run Insider using the **insider.bat** or **insider.sh** script):
```
insider.sh find config/code_smells.json
```

* You can also run the *find* command with both files at once: 
```
insider.sh find config/libraries.json config/code_smells.json
```

The commands will generate two *.json* files (**_PROJECT_ID-libraries.json_** and **_PROJECT_ID-code_smells.json_**) in the **results** folder.

## Acknowledgements

The `inspect` command is inspired by the [Application Inspector](https://github.com/microsoft/ApplicationInspector) project created by Microsoft.
Insider even uses the same input files as Application Inspector.
