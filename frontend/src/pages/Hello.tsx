import logo from './../assets/react.svg'
import './../App.css';
import {Link} from "react-router-dom";
import {useAuth} from "../context/AuthContext.tsx";

function Hello() {
    const { isLoggedIn } = useAuth();
  return (
    <div className="App">
      <header className="App-header">
          <img src={logo} className="App-logo" alt="logo" />
        <p>
            {!isLoggedIn && <Link to="/register">Register</Link>}
        </p>
      </header>
    </div>
  );
}

export default Hello;
