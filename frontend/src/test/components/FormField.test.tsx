import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { RadioCard, CollapsibleSection } from '../../components/FormField';

describe('RadioCard', () => {
  it('renders title and description', () => {
    render(
      <RadioCard
        name="test"
        value="a"
        checked={false}
        onChange={() => {}}
        title="Option A"
        description="Description A"
      />,
    );
    expect(screen.getByText('Option A')).toBeInTheDocument();
    expect(screen.getByText('Description A')).toBeInTheDocument();
  });

  it('shows recommended badge when recommended=true', () => {
    render(
      <RadioCard
        name="test"
        value="a"
        checked={false}
        onChange={() => {}}
        title="Option A"
        description="Desc"
        recommended
      />,
    );
    expect(screen.getByText('Recommended')).toBeInTheDocument();
  });

  it('applies selected styling when checked', () => {
    const { container } = render(
      <RadioCard
        name="test"
        value="a"
        checked={true}
        onChange={() => {}}
        title="Option A"
        description="Desc"
      />,
    );
    const label = container.querySelector('label');
    expect(label?.className).toContain('emerald');
  });
});

describe('CollapsibleSection', () => {
  it('renders title and children', () => {
    render(
      <CollapsibleSection title="Advanced">
        <div>Hidden content</div>
      </CollapsibleSection>,
    );
    expect(screen.getByText('Advanced')).toBeInTheDocument();
    expect(screen.getByText('Hidden content')).toBeInTheDocument();
  });

  it('renders badge when provided', () => {
    render(
      <CollapsibleSection title="Advanced" badge="3 items">
        <div>Content</div>
      </CollapsibleSection>,
    );
    expect(screen.getByText('3 items')).toBeInTheDocument();
  });
});
