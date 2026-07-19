import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import StatusBadge from '../../components/StatusBadge';

describe('StatusBadge', () => {
  it('renders the status text', () => {
    render(<StatusBadge status="RUNNING" />);
    expect(screen.getByText('RUNNING')).toBeInTheDocument();
  });

  it('renders a dot indicator', () => {
    const { container } = render(<StatusBadge status="RUNNING" />);
    const dot = container.querySelector('.rounded-full');
    expect(dot).toBeInTheDocument();
  });

  it('applies correct styling for RUNNING status', () => {
    const { container } = render(<StatusBadge status="RUNNING" />);
    const badge = container.querySelector('span');
    expect(badge?.className).toContain('emerald');
  });

  it('applies correct styling for FAILED status', () => {
    const { container } = render(<StatusBadge status="FAILED" />);
    const badge = container.querySelector('span');
    expect(badge?.className).toContain('red');
  });

  it('falls back to DRAFT styling for unknown status', () => {
    const { container } = render(<StatusBadge status="UNKNOWN" />);
    const badge = container.querySelector('span');
    expect(badge?.className).toContain('gray');
  });

  it('shows pulsing dot for RUNNING status', () => {
    const { container } = render(<StatusBadge status="RUNNING" />);
    const dot = container.querySelector('.animate-pulse');
    expect(dot).toBeInTheDocument();
  });
});
