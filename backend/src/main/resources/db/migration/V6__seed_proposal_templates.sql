CREATE TABLE proposal_templates (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) NOT NULL,
  overview TEXT,
  scope TEXT,
  timeline TEXT,
  pricing_notes TEXT,
  created_at TIMESTAMP DEFAULT now()
);

INSERT INTO proposal_templates (id, name, overview, scope, timeline, pricing_notes) VALUES
(gen_random_uuid(), 'Web Design Project',
  'We propose a complete website redesign to modernize your online presence and improve user experience.',
  '- Custom responsive design\n- Up to 10 pages\n- CMS integration\n- Basic SEO setup\n- 2 rounds of revisions',
  'Week 1–2: Discovery and wireframing\nWeek 3–4: UI/UX design\nWeek 5–8: Development and testing\nWeek 9: Launch and handover',
  '50% deposit required to begin. Remaining balance due on project completion.'),
(gen_random_uuid(), 'Brand Identity Package',
  'A comprehensive brand identity package to establish a strong, cohesive visual presence for your business.',
  '- Logo design (3 concepts)\n- Color palette and typography\n- Brand guidelines document\n- Business card design\n- Letterhead template',
  'Week 1: Brand discovery and research\nWeek 2: Concept development\nWeek 3: Design refinement\nWeek 4: Final delivery',
  'All source files included. Print-ready files provided. 2 rounds of revisions included.'),
(gen_random_uuid(), 'Monthly Retainer — Development',
  'Ongoing development and maintenance retainer to keep your product evolving with your business needs.',
  '- Up to 40 hours/month of development\n- Bug fixes and maintenance\n- Feature development\n- Code reviews\n- Monthly progress report',
  'Rolling monthly engagement. 30-day notice to cancel.',
  'Billed monthly in advance. Unused hours do not roll over. Priority support included.');
