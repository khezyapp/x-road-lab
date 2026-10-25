import { defineConfig } from 'vitepress'

// Add a page to the knowledge base once; it appears in both sidebars.
const knowledgeBase = [
  { text: 'Foundations', link: '/x-road/01-foundations' },
  { text: 'Identifiers', link: '/x-road/02-identifiers' },
  { text: 'Protocols and security', link: '/x-road/03-protocols-and-security' },
  { text: 'Registration and access', link: '/x-road/04-registration-and-access' },
  { text: 'Versions and CamDX', link: '/x-road/05-versions-and-camdx' },
  { text: 'Lab topology', link: '/x-road/06-lab-topology' },
  { text: 'Runbook (v7 Docker)', link: '/x-road/07-runbook-v7-docker' },
  { text: 'Spring Boot integration', link: '/x-road/08-spring-boot-integration' },
  { text: 'Troubleshooting', link: '/x-road/09-troubleshooting' },
  { text: 'Glossary', link: '/x-road/10-glossary' },
  { text: 'CamDX onboarding', link: '/x-road/11-camdx-onboarding' },
  { text: 'RFC 3161 time-stamping', link: '/x-road/12-rfc3161-timestamping' },
  { text: 'Member services (Spring Boot 4)', link: '/x-road/13-member-services' }
]

// The lab guide is ordered: steps run in sequence.
const labGuide = [
  { text: 'Overview', link: '/guide/' },
  { text: '01 — Prerequisites', link: '/guide/01-prerequisites' },
  { text: '02 — Get X-Road 7.8.3 source', link: '/guide/02-get-source' },
  { text: '03 — Start the containers', link: '/guide/03-start-stack' },
  { text: '04 — Initialize X-Road', link: '/guide/04-initialize' },
  { text: '05 — Open the admin UIs', link: '/guide/05-open-admin-ui' },
  { text: '06 — Inspect what initialization created', link: '/guide/06-inspect-state' },
  { text: '07 — First REST call', link: '/guide/07-first-rest-call' },
  { text: '08 — Read the response headers', link: '/guide/08-read-response-headers' },
  { text: '09 — First SOAP call', link: '/guide/09-first-soap-call' },
  { text: '10 — Failure lab', link: '/guide/10-failure-lab' },
  { text: '11 — Logs and evidence', link: '/guide/11-logs-and-evidence' },
  { text: '12 — Reset the lab', link: '/guide/12-reset' },
  { text: '13 — Next steps', link: '/guide/13-next-steps' }
]

export default defineConfig({
  lang: 'en-US',
  title: 'X-Road Lab',
  description:
    'SME-CU / CamDX X-Road 7.8.x learning lab: manual lab guide, knowledge base, and decisions.',
  titleTemplate: ':title — X-Road Lab',
  cleanUrls: false,
  lastUpdated: false,
  head: [['meta', { name: 'theme-color', content: '#0b6bcb' }]],

  // The guide links to lab endpoints (admin UIs, proxies) that only exist while
  // the X-Road stack is running. They are not pages of this site.
  ignoreDeadLinks: [/^https?:\/\/localhost/, /^https?:\/\/127\.0\.0\.1/],

  themeConfig: {
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Lab guide', link: '/guide/' },
      { text: 'Knowledge base', link: '/x-road/01-foundations' },
      { text: 'Decisions', link: '/decisions/0001-x-road-version' }
    ],

    sidebar: {
      '/guide/': [
        { text: 'Manual Lab Guide', items: labGuide },
        { text: 'Knowledge Base', items: knowledgeBase }
      ],

      '/x-road/': [
        { text: 'Knowledge Base', items: knowledgeBase },
        { text: 'Hands-on', items: [{ text: 'Manual lab guide', link: '/guide/' }] }
      ],

      '/decisions/': [
        {
          text: 'Decisions',
          items: [{ text: '0001 — Target X-Road 7.8.x', link: '/decisions/0001-x-road-version' }]
        },
        {
          text: 'Reference',
          items: [{ text: 'Knowledge base index', link: '/' }]
        }
      ]
    },

    search: {
      provider: 'local'
    },

    outline: {
      level: [2, 3],
      label: 'On this page'
    },

    docFooter: {
      prev: 'Previous page',
      next: 'Next page'
    },

    returnToTopLabel: 'Back to top',
    sidebarMenuLabel: 'Menu',
    darkModeSwitchLabel: 'Appearance',
    lightModeSwitchTitle: 'Switch to light theme',
    darkModeSwitchTitle: 'Switch to dark theme',

    footer: {
      message: 'Learning lab — disposable, test credentials, not production.',
      copyright: 'Summaries derived from X-Road documentation (CC BY-SA 3.0); upstream remains authoritative.'
    }
  }
})
