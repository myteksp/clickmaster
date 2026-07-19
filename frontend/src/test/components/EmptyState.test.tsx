import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import EmptyState from '../../components/EmptyState';

describe('EmptyState', () => {
  it('renders the title', () => {
    render(<EmptyState title="No campaigns yet" />);
    expect(screen.getByText('No campaigns yet')).toBeInTheDocument();
  });

  it('renders description when provided', () => {
    render(<EmptyState title="Empty" description="Create your first campaign" />);
    expect(screen.getByText('Create your first campaign')).toBeInTheDocument();
  });

  it('does not render description when not provided', () => {
    render(<EmptyState title="Empty" />);
    expect(screen.queryByText('Create your first campaign')).not.toBeInTheDocument();
  });

  it('renders action when provided', () => {
    render(
      <EmptyState title="Empty" action={<button>Create</button>} />,
    );
    expect(screen.getByRole('button', { name: 'Create' })).toBeInTheDocument();
  });
});
