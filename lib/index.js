const {insider} = require("./lib");
const {Command} = require("commander");

exports.insiderCommand = new Command()
  .name('insider')
  .description('Running Insider commands')
  .option('-wd --working-directory', 'Selects the directory where Insider will store the results folder.' +
    ` Defaults to the location where Insider is installed: ${__dirname}. If set to true it will use the current working directory process.cwd()`,
    false)
  .allowUnknownOption()
  .action(insider)


