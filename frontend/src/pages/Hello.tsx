import logo from './../assets/react.svg'
import './../App.css';
import {Link} from "react-router-dom";
import {useAuth} from "../context/AuthContext.tsx";

function Hello() {
    const { user } = useAuth();
  return (
    <div className="App">
      <header className="App-header">
          <img src={logo} className="App-logo" alt="logo" />
        <p>
            {!user && <Link to="/register">Register</Link>}
        </p>
      </header>
    </div>
  );
}

export default Hello;
