import logo from './../assets/react.svg'
import './../App.css';
import {isLoggedIn} from "../utils/auth.ts";
import {Link} from "react-router-dom";

function Hello() {
  return (
    <div className="App">
      <header className="App-header">
          <img src={logo} className="App-logo" alt="logo" />
        <p>
            {!isLoggedIn() && <Link to="/register">Register</Link>}
        </p>
      </header>
    </div>
  );
}

export default Hello;
