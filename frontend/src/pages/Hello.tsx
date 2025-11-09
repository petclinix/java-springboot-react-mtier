import React, {useEffect} from 'react';
import logo from './../logo.svg';
import './../App.css';

function Hello() {
    let [greeting, setGreeting] = React.useState<string>('');
    useEffect(() => {
        fetch('/api/hello')
        .then(response => response.json())
        .then(data => {
          console.log(data);
          setGreeting(data.content);
        });
    }, []);
  return (
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo" />
        <p>
          Edit <code>src/App.tsx</code> and save to reload.
        </p>
        <p>{greeting}</p>
        <a
          className="App-link"
          href="https://reactjs.org"
          target="_blank"
          rel="noopener noreferrer"
        >
          Learn React
        </a>
      </header>
    </div>
  );
}

export default Hello;
