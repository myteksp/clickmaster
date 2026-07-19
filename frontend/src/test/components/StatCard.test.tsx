import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import StatCard from '../../components/StatCard';

describe('StatCard', () => {
  it('renders label and value', () => {
    render(<StatCard label="Total Visits" value={1234} />);
    expect(screen.getByText('Total Visits')).toBeInTheDocument();
    expect(screen.getByText('1234')).toBeInTheDocument();
  });

  it('renders subtext when provided', () => {
    render(<StatCard label="Rate" value="500/h" subtext="Peak: 750/h" />);
    expect(screen.getByText('Peak: 750/h')).toBeInTheDocument();
  });

  it('does not render subtext when not provided', () => {
    render(<StatCard label="Rate" value="500/h" />);
    expect(screen.queryByText('Peak')).not.toBeInTheDocument();
  });

  it('applies custom color class', () => {
    render(<StatCard label="Status" value="OK" color="text-emerald-400" />);
    const value = screen.getByText('OK');
    expect(value.className).toContain('text-emerald-400');
  });
});
