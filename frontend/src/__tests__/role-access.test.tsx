import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from '../App';

describe('Role-based Access and Navigation Flows', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('renders guest view with sign-in trigger and default dashboard', () => {
    render(<App />);

    expect(screen.getByText('RUSHINGA PROVINCIAL')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: /Disaster Monitoring/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sign in/i })).toBeInTheDocument();

    // Guest sees dashboard
    expect(screen.getByText(/Approved incidents at a glance/i)).toBeInTheDocument();

    // Navigation tabs are hidden when not signed in
    expect(screen.queryByRole('button', { name: /^record$/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^review$/i })).not.toBeInTheDocument();
  });

  it('allows user to sign in as Ward recorder with assigned ward and enables record/my-incidents tabs', async () => {
    const user = userEvent.setup();
    render(<App />);

    // Open sign in dialog
    await user.click(screen.getByRole('button', { name: /Sign in/i }));

    expect(screen.getByRole('dialog', { name: /Sign in/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/Assigned ward/i)).toBeInTheDocument();

    // Fill sign in form
    await user.type(screen.getByLabelText(/Name/i), 'Tariro Moyo');
    await user.click(screen.getByRole('button', { name: /Continue/i }));

    // Signed in banner
    expect(screen.getByText(/Tariro Moyo · Ward recorder/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sign out/i })).toBeInTheDocument();

    // Navigation permissions
    const recordBtn = screen.getByRole('button', { name: /^record$/i });
    const myIncidentsBtn = screen.getByRole('button', { name: /^my incidents$/i });
    const reviewBtn = screen.getByRole('button', { name: /^review$/i });

    expect(recordBtn).toBeEnabled();
    expect(myIncidentsBtn).toBeEnabled();
    expect(reviewBtn).toBeDisabled();

    // Switch to record tab
    await user.click(recordBtn);
    expect(screen.getByText(/WARD CAPTURE · FLOOD/i)).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: /New Flood incident/i })).toBeInTheDocument();
  });

  it('enables review tab and disables record tab for Provincial supervisor', () => {
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

    expect(screen.getByText(/Supervisor Sarah · Provincial supervisor/i)).toBeInTheDocument();

    const recordBtn = screen.getByRole('button', { name: /^record$/i });
    const reviewBtn = screen.getByRole('button', { name: /^review$/i });

    expect(recordBtn).toBeDisabled();
    expect(reviewBtn).toBeEnabled();

    // Can navigate to review queue
    fireEvent.click(reviewBtn);
    expect(screen.getByText(/PROVINCIAL REVIEW QUEUE/i)).toBeInTheDocument();
  });

  it('enables review tab for Provincial administrator', () => {
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

    expect(screen.getByText(/Admin Nyasha · Provincial administrator/i)).toBeInTheDocument();

    const recordBtn = screen.getByRole('button', { name: /^record$/i });
    const reviewBtn = screen.getByRole('button', { name: /^review$/i });

    expect(recordBtn).toBeDisabled();
    expect(reviewBtn).toBeEnabled();
  });

  it('enforces read-only access for National viewer: both record and review are disabled', () => {
    localStorage.setItem(
      'dpdms-session',
      JSON.stringify({
        name: 'Viewer Simba',
        role: 'National viewer',
        hazard: 'Flood',
        ward: 'Ward 1',
      })
    );

    render(<App />);

    expect(screen.getByText(/Viewer Simba · National viewer/i)).toBeInTheDocument();

    const recordBtn = screen.getByRole('button', { name: /^record$/i });
    const reviewBtn = screen.getByRole('button', { name: /^review$/i });
    const dashboardBtn = screen.getByRole('button', { name: /^dashboard$/i });
    const reportsBtn = screen.getByRole('button', { name: /^reports$/i });

    expect(recordBtn).toBeDisabled();
    expect(reviewBtn).toBeDisabled();
    expect(dashboardBtn).toBeEnabled();
    expect(reportsBtn).toBeEnabled();
  });

  it('handles sign out correctly', async () => {
    const user = userEvent.setup();
    localStorage.setItem(
      'dpdms-session',
      JSON.stringify({
        name: 'Tariro Moyo',
        role: 'Ward recorder',
        hazard: 'Flood',
        ward: 'Ward 1',
      })
    );

    render(<App />);
    expect(screen.getByText(/Tariro Moyo · Ward recorder/i)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Sign out/i }));

    expect(screen.queryByText(/Tariro Moyo · Ward recorder/i)).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sign in/i })).toBeInTheDocument();
    expect(localStorage.getItem('dpdms-session')).toBeNull();
  });
});

