const path = require('node:path');
const {spawnSync} = require('node:child_process');
const {JavaCaller} = require('java-caller');

function getInsiderArgs() {
  const args = [...process.argv];
  let index = args.indexOf('insider');
  if (index === -1)
    index = 1;
  args.splice(0, index + 1);
  return args;
}

function runSummaryScript(options, targetDirectoryArg) {
  const targetDirectory = path.resolve(process.cwd(), targetDirectoryArg || 'results');
  const pythonScript = path.join(__dirname, 'insider-summary.py');

  const preferredCommands = process.platform === 'win32'
    ? [
      {command: 'py', args: ['-3']},
      {command: 'python', args: []},
      {command: 'python3', args: []}
    ]
    : [
      {command: 'python3', args: []},
      {command: 'python', args: []}
    ];

  for (const entry of preferredCommands) {
    const result = spawnSync(entry.command, [...entry.args, pythonScript, targetDirectory], {
      cwd: options?.workingDirectory ? process.cwd() : __dirname,
      stdio: 'inherit'
    });

    if (result.error && result.error.code === 'ENOENT')
      continue;

    if (result.error) {
      console.error(`summary generation failed for '${targetDirectory}': ${result.error.message}`);
      process.exitCode = 1;
      return;
    }

    process.exitCode = result.status ?? 1;
    return;
  }

  console.error("summary generation failed: could not find a Python interpreter ('py -3', 'python3' or 'python')");
  process.exitCode = 1;
}

async function insider(options) {
  const args = getInsiderArgs();
  if (args[0] === 'summary') {
    runSummaryScript(options, args[1]);
    return;
  }

  const java = new JavaCaller({
    jar: 'insider.jar', // CLASSPATH referencing the package embedded jar files
    mainClass: 'org.dxworks.insider.Insider',// Main class to call, must be available from CLASSPATH,
    rootPath: __dirname,
    minimumJavaVersion: 11,
    output: 'console'
  });

  const {status} = await java.run(args, {cwd: options?.workingDirectory? process.cwd(): __dirname});
  process.exitCode = status;
}

module.exports = {insider}
