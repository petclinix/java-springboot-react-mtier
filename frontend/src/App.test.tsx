import { render, screen } from '@testing-library/react';
import App from './App';

test('renders Vite + React', () => {
  render(<App />);
  const homeElement = screen.getByText("Home");
  expect(homeElement).toBeInTheDocument();
});
