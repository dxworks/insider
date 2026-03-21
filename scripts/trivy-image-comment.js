const fs = require('fs');

module.exports = async ({ github, context }) => {
  const sarifFile = 'trivy-image-results.sarif';
  const marker = '## Trivy Docker Image Scan';

  if (!fs.existsSync(sarifFile)) {
    console.log('No SARIF file found, skipping comment.');
    return;
  }

  const sarif = JSON.parse(fs.readFileSync(sarifFile, 'utf8'));
  const results = sarif.runs.flatMap(run => run.results || []);

  const prNumber = context.issue.number;
  const repoUrl = `https://github.com/${context.repo.owner}/${context.repo.repo}`;
  const securityUrl = `${repoUrl}/security/code-scanning?query=is%3Aopen+ref%3Arefs%2Fpull%2F${prNumber}%2Fmerge+tool%3ATrivy`;

  let body;

  if (results.length === 0) {
    body = `${marker}\n\n:white_check_mark: No vulnerabilities found in the Docker image.`;
  } else {
    const severityEmoji = { error: ':red_circle:', warning: ':orange_circle:', note: ':yellow_circle:' };

    const rows = results.map(r => {
      const emoji = severityEmoji[r.level] || ':white_circle:';
      const desc = (r.message?.text || '').split('\n')[0].substring(0, 120);
      const cveId = r.ruleId;
      const cveLink = cveId.startsWith('CVE-')
        ? `[${cveId}](https://avd.aquasec.com/nvd/${cveId.toLowerCase()})`
        : cveId.startsWith('GHSA-')
          ? `[${cveId}](https://github.com/advisories/${cveId})`
          : `\`${cveId}\``;
      return `| ${emoji} ${r.level || 'unknown'} | ${cveLink} | ${desc} |`;
    });

    body = [
      `${marker}`,
      '',
      `:rotating_light: Found **${results.length}** vulnerability(ies) in the Docker image built from the \`Dockerfile\`.`,
      '',
      '| Severity | CVE | Description |',
      '|----------|-----|-------------|',
      ...rows,
      '',
      `[:mag: View all findings in the Security tab](${securityUrl})`,
      '',
      '> These are OS-level vulnerabilities in the container base image. Consider updating the base image or adding `RUN apk upgrade --no-cache` to the Dockerfile.'
    ].join('\n');
  }

  const comments = await github.rest.issues.listComments({
    owner: context.repo.owner,
    repo: context.repo.repo,
    issue_number: prNumber
  });
  const existing = comments.data.find(c => c.body.includes(marker));

  if (existing) {
    await github.rest.issues.updateComment({
      owner: context.repo.owner,
      repo: context.repo.repo,
      comment_id: existing.id,
      body
    });
  } else {
    await github.rest.issues.createComment({
      owner: context.repo.owner,
      repo: context.repo.repo,
      issue_number: prNumber,
      body
    });
  }
};
