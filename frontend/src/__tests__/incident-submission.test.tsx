import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from '../App';

describe('Incident Submission and Editing Workflows', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('submits a new flood incident as a Ward recorder with correct indicators and audit entry', async () => {
    const user = userEvent.setup();

    // Sign in as Ward recorder for Flood in Ward 3
    localStorage.setItem(
      'dpdms-session',
      JSON.stringify({
        name: 'Tatenda Chidziwa',
        role: 'Ward recorder',
        hazard: 'Flood',
        ward: 'Ward 3',
      })
    );

    render(<App />);

    // Navigate to record tab
    await user.click(screen.getByRole('button', { name: /^record$/i }));

    // Verify assigned ward is locked
    const wardInput = screen.getByLabelText(/^Ward$/i) as HTMLInputElement;
    expect(wardInput).toBeDisabled();
    expect(wardInput.value).toBe('Ward 3');

    // Fill common incident fields
    await user.type(screen.getByLabelText(/District/i), 'Rushinga');
    await user.type(screen.getByLabelText(/Date and time/i), '2026-09-21T09:30');
    await user.selectOptions(screen.getByLabelText(/Severity/i), 'HIGH');
    await user.type(screen.getByLabelText(/Latitude/i), '-17.15');
    await user.type(screen.getByLabelText(/Longitude/i), '32.45');

    // Fill flood-specific indicator fields
    await user.type(screen.getByLabelText(/Peak water level \(metres\)/i), '2.8');
    await user.type(screen.getByLabelText(/River basin \/ catchment/i), 'Mazowe Basin');
    await user.type(screen.getByLabelText(/Households displaced/i), '12');
    await user.type(screen.getByLabelText(/Area flooded \(hectares\)/i), '35.5');
    await user.type(screen.getByLabelText(/Inundation duration \(days\)/i), '4');

    // Submit the form
    await user.click(screen.getByRole('button', { name: /Submit for approval/i }));

    // Flash message and auto-transition to my-incidents
    expect(screen.getByText(/Incident submitted to the relevant provincial supervisor/i)).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: /Incident status/i })).toBeInTheDocument();

    // Verify incident listing in my-incidents
    expect(screen.getByText(/Flood · PENDING/i)).toBeInTheDocument();
    expect(screen.getByText(/Ward 3, Rushinga · HIGH/i)).toBeInTheDocument();
    expect(screen.getByText(/Submitted for approval/i)).toBeInTheDocument();

    // Verify saved to localStorage
    const savedIncidents = JSON.parse(localStorage.getItem('dpdms-incidents') || '[]');
    expect(savedIncidents).toHaveLength(1);
    expect(savedIncidents[0].hazard).toBe('Flood');
    expect(savedIncidents[0].ward).toBe('Ward 3');
    expect(savedIncidents[0].status).toBe('PENDING');
    expect(savedIncidents[0].reporter).toBe('Tatenda Chidziwa');
    expect(savedIncidents[0].fields.peakWaterLevelMetres).toBe('2.8');
    expect(savedIncidents[0].fields.riverBasin).toBe('Mazowe Basin');
    expect(savedIncidents[0].audit[0].action).toBe('Submitted for approval');
  });

  it('renders correct dynamic indicators for Fire hazard', async () => {
    const user = userEvent.setup();

    localStorage.setItem(
      'dpdms-session',
      JSON.stringify({
        name: 'Farai Moyo',
        role: 'Ward recorder',
        hazard: 'Fire',
        ward: 'Ward 5',
      })
    );

    render(<App />);

    await user.click(screen.getByRole('button', { name: /^record$/i }));

    expect(screen.getByText(/WARD CAPTURE · FIRE/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Area burned \(hectares\)/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Suspected cause/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Injuries \/ fatalities/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Structures destroyed/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Still active or contained/i)).toBeInTheDocument();
  });

  it('allows editing and resubmission of an incident marked CORRECTION_REQUESTED', async () => {
    const user = userEvent.setup();

    const existingIncident = {
      id: 'incident-101',
      hazard: 'Flood',
      ward: 'Ward 1',
      district: 'Rushinga',
      occurredAt: '2026-09-20T08:00',
      severity: 'MEDIUM',
      latitude: -17.2,
      longitude: 32.3,
      status: 'CORRECTION_REQUESTED',
      reporter: 'Tariro Moyo',
      fields: {
        peakWaterLevelMetres: '1.5',
        riverBasin: 'Mazowe',
        householdsDisplaced: '2',
        floodedAreaHectares: '10',
        inundationDays: '1',
      },
      audit: [
        { at: '2026-09-20T08:10:00Z', actor: 'Tariro Moyo', action: 'Submitted for approval' },
        { at: '2026-09-20T09:00:00Z', actor: 'Supervisor Sarah', action: 'CORRECTION_REQUESTED', reason: 'Please verify peak water level' },
      ],
    };

    localStorage.setItem(
      'dpdms-session',
      JSON.stringify({
        name: 'Tariro Moyo',
        role: 'Ward recorder',
        hazard: 'Flood',
        ward: 'Ward 1',
      })
    );
    localStorage.setItem('dpdms-incidents', JSON.stringify([existingIncident]));

    render(<App />);

    // Go to my-incidents
    await user.click(screen.getByRole('button', { name: /^my incidents$/i }));

    expect(screen.getByText(/Flood · CORRECTION_REQUESTED/i)).toBeInTheDocument();
    expect(screen.getByText(/Please verify peak water level/i)).toBeInTheDocument();

    // Click Edit button
    await user.click(screen.getByRole('button', { name: /^Edit$/i }));

    // Form title switches to Correct and resubmit
    expect(screen.getByRole('heading', { level: 2, name: /Correct and resubmit/i })).toBeInTheDocument();

    // Update peak water level
    const peakInput = screen.getByLabelText(/Peak water level \(metres\)/i);
    await user.clear(peakInput);
    await user.type(peakInput, '2.1');

    // Submit corrections
    await user.click(screen.getByRole('button', { name: /Save and resubmit/i }));

    expect(screen.getByText(/Corrections saved and the incident has been resubmitted/i)).toBeInTheDocument();

    // Verify incident updated to PENDING with new audit entry
    const saved = JSON.parse(localStorage.getItem('dpdms-incidents') || '[]');
    expect(saved[0].status).toBe('PENDING');
    expect(saved[0].fields.peakWaterLevelMetres).toBe('2.1');
    expect(saved[0].audit[2].action).toBe('Corrected and resubmitted');
    expect(saved[0].audit[2].actor).toBe('Tariro Moyo');
  });

  it('allows recorder to delete a pending incident after confirmation', async () => {
    const user = userEvent.setup();
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

    const pendingIncident = {
      id: 'incident-202',
      hazard: 'Flood',
      ward: 'Ward 1',
      district: 'Rushinga',
      occurredAt: '2026-09-20T08:00',
      severity: 'LOW',
      latitude: -17.2,
      longitude: 32.3,
      status: 'PENDING',
      reporter: 'Tariro Moyo',
      fields: {
        peakWaterLevelMetres: '0.8',
        riverBasin: 'Mazowe',
        householdsDisplaced: '0',
        floodedAreaHectares: '2',
        inundationDays: '1',
      },
      audit: [{ at: '2026-09-20T08:00:00Z', actor: 'Tariro Moyo', action: 'Submitted' }],
    };

    localStorage.setItem(
      'dpdms-session',
      JSON.stringify({
        name: 'Tariro Moyo',
        role: 'Ward recorder',
        hazard: 'Flood',
        ward: 'Ward 1',
      })
    );
    localStorage.setItem('dpdms-incidents', JSON.stringify([pendingIncident]));

    render(<App />);

    await user.click(screen.getByRole('button', { name: /^my incidents$/i }));
    expect(screen.getByText(/Flood · PENDING/i)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /^Delete$/i }));

    expect(confirmSpy).toHaveBeenCalledWith('Delete this pending incident? This cannot be undone.');
    expect(screen.getByText(/Pending incident deleted/i)).toBeInTheDocument();
    expect(screen.queryByText(/Flood · PENDING/i)).not.toBeInTheDocument();

    const saved = JSON.parse(localStorage.getItem('dpdms-incidents') || '[]');
    expect(saved).toHaveLength(0);

    confirmSpy.mockRestore();
  });
});

