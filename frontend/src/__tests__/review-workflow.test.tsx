import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from '../App';

describe('Supervisor Review, Decision and Dashboard Isolation Workflow', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  const floodIncident = {
    id: 'flood-1',
    hazard: 'Flood' as const,
    ward: 'Ward 2',
    district: 'Rushinga',
    occurredAt: '2026-09-21T06:00',
    severity: 'CRITICAL',
    latitude: -17.15,
    longitude: 32.45,
    status: 'PENDING' as const,
    reporter: 'Tariro Moyo',
    fields: {
      peakWaterLevelMetres: '3.4',
      riverBasin: 'Mazowe',
      householdsDisplaced: '25',
      floodedAreaHectares: '60',
      inundationDays: '5',
    },
    audit: [{ at: '2026-09-21T06:30:00Z', actor: 'Tariro Moyo', action: 'Submitted for approval' }],
  };

  const fireIncident = {
    id: 'fire-1',
    hazard: 'Fire' as const,
    ward: 'Ward 4',
    district: 'Rushinga',
    occurredAt: '2026-09-21T07:00',
    severity: 'HIGH',
    latitude: -17.1,
    longitude: 32.5,
    status: 'PENDING' as const,
    reporter: 'Farai Moyo',
    fields: {
      areaBurnedHectares: '15',
      suspectedCause: 'Veld fire',
      casualties: '0',
      structuresDestroyed: '1',
      fireState: 'active',
    },
    audit: [{ at: '2026-09-21T07:15:00Z', actor: 'Farai Moyo', action: 'Submitted for approval' }],
  };

  it('restricts review queue to assigned hazard for supervisor and shows all hazards for administrator', async () => {
    const user = userEvent.setup();
    localStorage.setItem('dpdms-incidents', JSON.stringify([floodIncident, fireIncident]));

    // Sign in as Flood supervisor
    localStorage.setItem(
      'dpdms-session',
      JSON.stringify({
        name: 'Supervisor Sarah',
        role: 'Provincial supervisor',
        hazard: 'Flood',
        ward: 'Ward 1',
      })
    );

    const { unmount } = render(<App />);

    // Go to review tab
    await user.click(screen.getByRole('button', { name: /^review$/i }));

    // Only Flood incident is visible in review queue
    expect(screen.getByText(/Flood · CRITICAL/i)).toBeInTheDocument();
    expect(screen.queryByText(/Fire · HIGH/i)).not.toBeInTheDocument();

    unmount();

    // Now switch to Provincial Administrator
    localStorage.setItem(
      'dpdms-session',
      JSON.stringify({
        name: 'Admin Nyasha',
        role: 'Provincial administrator',
        hazard: 'Flood',
        ward: 'Ward 1',
      })
    );

    render(<App />);
    await user.click(screen.getByRole('button', { name: /^review$/i }));

    // Administrator sees both hazards in review queue
    expect(screen.getByText(/Flood · CRITICAL/i)).toBeInTheDocument();
    expect(screen.getByText(/Fire · HIGH/i)).toBeInTheDocument();
  });

  it('approves an incident and promotes it to the public dashboard and reports', async () => {
    const user = userEvent.setup();
    localStorage.setItem('dpdms-incidents', JSON.stringify([floodIncident]));
    localStorage.setItem(
      'dpdms-session',
      JSON.stringify({
        name: 'Supervisor Sarah',
        role: 'Provincial supervisor',
        hazard: 'Flood',
        ward: 'Ward 1',
      })
    );

    render(<App />);

    // Review tab
    await user.click(screen.getByRole('button', { name: /^review$/i }));
    expect(screen.getByText(/Flood · CRITICAL/i)).toBeInTheDocument();

    // Click Approve button
    await user.click(screen.getByRole('button', { name: /^Approve$/i }));

    expect(screen.getByText(/Incident approved/i)).toBeInTheDocument();
    expect(screen.getByText(/No incidents require your attention/i)).toBeInTheDocument();

    // Verify localStorage has status APPROVED and updated audit
    const saved = JSON.parse(localStorage.getItem('dpdms-incidents') || '[]');
    expect(saved[0].status).toBe('APPROVED');
    expect(saved[0].audit[1].action).toBe('APPROVED');
    expect(saved[0].audit[1].reason).toBe('Approved for publication');

    // Check Dashboard reflects the approved incident
    await user.click(screen.getByRole('button', { name: /^dashboard$/i }));
    expect(screen.getByText('Floods')).toBeInTheDocument();
    expect(screen.getByText(/Ward 2 · CRITICAL/i)).toBeInTheDocument();

    // Check Reports reflects the approved incident
    await user.click(screen.getByRole('button', { name: /^reports$/i }));
    expect(screen.getByText(/1 approved incident\(s\) match this filter/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Download CSV/i })).toBeEnabled();

    // Check Alerts tab includes critical flood
    await user.click(screen.getByRole('button', { name: /^alerts$/i }));
    expect(screen.getByText(/Flood alert/i)).toBeInTheDocument();
    expect(screen.getByText(/Ward 2 · CRITICAL/i)).toBeInTheDocument();
  });

  it('requests correction with mandatory reason and transitions incident to CORRECTION_REQUESTED', async () => {
    const user = userEvent.setup();
    localStorage.setItem('dpdms-incidents', JSON.stringify([floodIncident]));
    localStorage.setItem(
      'dpdms-session',
      JSON.stringify({
        name: 'Supervisor Sarah',
        role: 'Provincial supervisor',
        hazard: 'Flood',
        ward: 'Ward 1',
      })
    );

    render(<App />);

    await user.click(screen.getByRole('button', { name: /^review$/i }));

    // Click Request correction
    await user.click(screen.getByRole('button', { name: /Request correction/i }));

    // Decision form appears
    expect(screen.getByRole('heading', { level: 3, name: /Request correction/i })).toBeInTheDocument();

    const reasonInput = screen.getByPlaceholderText(/Explain the decision clearly for the recorder/i);
    await user.type(reasonInput, 'Please confirm water level gauge reading at bridge.');

    await user.click(screen.getByRole('button', { name: /Confirm correction request/i }));

    expect(screen.getByText(/Incident correction requested/i)).toBeInTheDocument();
    expect(screen.getByText(/No incidents require your attention/i)).toBeInTheDocument();

    // Status updated in storage
    const saved = JSON.parse(localStorage.getItem('dpdms-incidents') || '[]');
    expect(saved[0].status).toBe('CORRECTION_REQUESTED');
    expect(saved[0].audit[1].action).toBe('CORRECTION_REQUESTED');
    expect(saved[0].audit[1].reason).toBe('Please confirm water level gauge reading at bridge.');
  });

  it('rejects an incident with mandatory reason and transitions incident to REJECTED', async () => {
    const user = userEvent.setup();
    localStorage.setItem('dpdms-incidents', JSON.stringify([floodIncident]));
    localStorage.setItem(
      'dpdms-session',
      JSON.stringify({
        name: 'Supervisor Sarah',
        role: 'Provincial supervisor',
        hazard: 'Flood',
        ward: 'Ward 1',
      })
    );

    render(<App />);

    await user.click(screen.getByRole('button', { name: /^review$/i }));

    // Click Reject
    await user.click(screen.getByRole('button', { name: /^Reject$/i }));

    expect(screen.getByRole('heading', { level: 3, name: /Reject incident/i })).toBeInTheDocument();

    const reasonInput = screen.getByPlaceholderText(/Explain the decision clearly for the recorder/i);
    await user.type(reasonInput, 'Duplicate entry of Ward 2 morning alert.');

    await user.click(screen.getByRole('button', { name: /Confirm rejection/i }));

    expect(screen.getByText(/Incident rejected/i)).toBeInTheDocument();
    expect(screen.getByText(/No incidents require your attention/i)).toBeInTheDocument();

    const saved = JSON.parse(localStorage.getItem('dpdms-incidents') || '[]');
    expect(saved[0].status).toBe('REJECTED');
    expect(saved[0].audit[1].action).toBe('REJECTED');
    expect(saved[0].audit[1].reason).toBe('Duplicate entry of Ward 2 morning alert.');
  });
});

