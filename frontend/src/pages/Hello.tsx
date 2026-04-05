import { Link } from "react-router-dom";
import { PageLayout } from "../components/ui/PageLayout";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";

function Hello() {
  return (
    <PageLayout narrow>
      <div style={{ textAlign: "center", padding: "48px 0 32px" }}>
        <div style={{ fontSize: 48, marginBottom: 16 }}>🐾</div>
        <h1 style={{ fontSize: 36, fontWeight: 800, color: "var(--color-text)", marginBottom: 12 }}>
          PetcliniX
        </h1>
        <p style={{ color: "var(--color-text-muted)", fontSize: 17, marginBottom: 40, maxWidth: 420, margin: "0 auto 40px" }}>
          Veterinary clinic management for pet owners and veterinarians.
        </p>
        <div style={{ display: "flex", gap: 12, justifyContent: "center", marginBottom: 48 }}>
          <Link to="/login"><Button variant="primary" size="md">Sign in</Button></Link>
          <Link to="/register"><Button variant="secondary" size="md">Create account</Button></Link>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 16 }}>
        {[
          { icon: "🐶", title: "Pet Owners", desc: "Register your pets and book appointments with trusted vets." },
          { icon: "🩺", title: "Veterinarians", desc: "Manage your locations, schedule, and document visits." },
          { icon: "📊", title: "Administrators", desc: "Monitor clinic activity and manage user accounts." },
        ].map(({ icon, title, desc }) => (
          <Card key={title} style={{ textAlign: "center" }}>
            <div style={{ fontSize: 32, marginBottom: 10 }}>{icon}</div>
            <h3 style={{ margin: "0 0 6px", fontSize: 15, fontWeight: 700 }}>{title}</h3>
            <p style={{ margin: 0, fontSize: 13, color: "var(--color-text-muted)", lineHeight: 1.5 }}>{desc}</p>
          </Card>
        ))}
      </div>
    </PageLayout>
  );
}

export default Hello;
