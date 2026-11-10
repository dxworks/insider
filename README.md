# Insider

Insider is a belt of tools built on the idea of searching regular expressions in code. Insider is able to detect code
smells, the usage of external libraries and the topics that a software project approaches by only using Regex. This
means that Insider does not use any type of parsing and is therefore language independent.

## Installation

### From Github

To install Insider from Github, please download from the [latest release](https://github.com/dxworks/insider/releases)
from Github, the `insider.zip` archive and unzip it to a specific location. The contents of this archive are:

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

* In order to find the occurrences of the libraries in the analyzed project. Use the following command (run Insider
  using the **insider.bat** or **insider.sh** script):

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

The commands will generate two *.json* files (**_PROJECT_ID-libraries.json_** and **_PROJECT_ID-code_smells.json_**) in
the **results** folder.

### Summary Command

Insider also provides a summary command for Voyager-compatible artifacts:

```
insider summary <path/to/results>
```

It reads `*-cloc.csv` files from the target results folder and generates:

- `summary.md` (frontmatter metadata + markdown)
- `summary.html` (rendered HTML section)

## Voyager Integration

Insider is also a Voyager Instrument. To configure Insider from Voyager you can add the following fields in the `mission.yml` file:

```yaml
# A map of instrument names to commands and parameters.
# When 'runsAll' is false the mission will run only the instruments
# with the commands declared here, in this order.
instruments:
  insider:
    # A map of parameter name to value
    # Only add the parameters you want to override (the default values are written here)
    parameters:
      max-heap: 4g # will configure the maximum heap space the jvm process will get. For large process may be needed to be set to 16g or higher
      findConfig: 'config/fingerprints/code_smells.json config/fingerprints/libraries.json' # a space separated list of insider fingerprints
      inspectConfig: 'config/rules' # a space separated list of folders or Application Inspector specific rules, like the ones here: https://github.com/microsoft/ApplicationInspector/tree/main/AppInspector/rules/default

# A map of environment variables, name to value, for voyager missions
# overwrites the variables from global config, instrument and command
# Only set the environment variables you need.
environment:
  INSIDER_LANGUAGES: 'java,c++,c#' # a comma separated list of the languages to analyse (just a sample example)
  INSIDER_LINGUIST_FILE: "/absolute/path/to/languages.yml" # a file containing languages to extension mappings according to [GitHub Linguist](https://github.com/github/linguist/blob/master/lib/linguist/languages.yml), see "Use your own languages.yml" below
  INSIDER_DEPEXT_MAX_NAMESPACE_LENGTH: 200 # the maximum length of a namespace for DepExt analysis
```

### Use your own languages.yml

Insider uses a `languages.yml` file to decide which files are source code: only files whose extension belongs to a programming language go through `detect`, `extract`, `inspect` and `find`. The file is in the [GitHub Linguist](https://github.com/github/linguist/blob/master/lib/linguist/languages.yml) format: each language with its type, extensions and file names. By default Insider uses the `languages.yml` shipped next to `insider.jar`, which comes from [dx-linguist](https://github.com/dxworks/dx-linguist).

To use your own file in Voyager (V1 and V2), set `INSIDER_LINGUIST_FILE` in the `environment:` section of `mission.yml`, with an absolute path. The analysis and the Insider summary (Voyager V2) then both use it:

```yaml
environment:
  INSIDER_LINGUIST_FILE: /absolute/path/to/languages.yml
```

In Voyager V2 you can also set it for Insider only. Set it on both steps, `start` (the analysis) and `summary`: a variable set on one step does not reach the other.

```yaml
instruments:
  insider:
    actions:
      start:
        environment:
          INSIDER_LINGUIST_FILE: /absolute/path/to/languages.yml
      summary:
        environment:
          INSIDER_LINGUIST_FILE: /absolute/path/to/languages.yml
```

- Use an absolute path. In the mission, `${instrument}` is not the Insider folder: Voyager V1 passes it on as plain text, and Voyager V2 replaces it with the instrument name. In Voyager V2 only, `${instrumentPath}/languages.yml` points to a file placed in the Insider instrument folder.
- If the file does not exist, or its name does not end in `.yml` or `.yaml`, Insider silently uses `~/.linguist/languages.yml` instead (downloaded from GitHub Linguist the first time). If the results do not change, check the path.
- When an extension is listed under several languages, the analysis treats the file as code if any of them is a programming language. The summary shows the first of them in the file.

Without Voyager, Insider takes the `INSIDER_LINGUIST_FILE` environment variable, then `linguist.file=<path>` in `config/insider-conf.properties`. Otherwise it uses `languages.yml` in the working folder. Relative paths are relative to the working folder. In Voyager, Insider's `instrument.yml` sets `INSIDER_LINGUIST_FILE` to the shipped file, so use the mission's `environment:` there: the properties file is not read.

## Acknowledgements

The `inspect` command is inspired by the [Application Inspector](https://github.com/microsoft/ApplicationInspector)
project created by Microsoft. Insider even uses the same input files as Application Inspector.
