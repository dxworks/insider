const {JavaCaller} = require('java-caller');

async function insider(options) {
  const java = new JavaCaller({
    jar: 'insider.jar', // CLASSPATH referencing the package embedded jar files
    mainClass: 'org.dxworks.insider.Insider',// Main class to call, must be available from CLASSPATH,
    rootPath: __dirname,
    minimumJavaVersion: 11,
    output: 'console'
  });

  const args = [...process.argv];
  let index = args.indexOf('insider'); //if it is called from dxw cli
  if(index === -1)
    index = 1
  args.splice(0,  index + 1);
  const {status} = await java.run(args, {cwd: options?.workingDirectory? process.cwd(): __dirname});
  process.exitCode = status;
}

module.exports = {insider}
