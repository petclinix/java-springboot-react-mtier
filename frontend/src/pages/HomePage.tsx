import { Link } from "react-router-dom";
import { PageLayout } from "../components/ui/PageLayout";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";

function HomePage() {
  return (
    <PageLayout narrow>
      <div className="text-center pt-[48px] pb-[32px]">
        <div className="text-[48px] mb-[16px]">🐾</div>
        <h1 className="text-[36px] font-extrabold text-[#1e293b] mb-[12px]">
          PetcliniX
        </h1>
        <p className="text-muted text-[17px] max-w-[420px] mx-auto mb-[40px]">
          Veterinary clinic management for pet owners and veterinarians.
        </p>
        <div className="flex gap-[12px] justify-center mb-[48px]">
          <Link to="/login"><Button variant="primary" size="md">Sign in</Button></Link>
          <Link to="/register"><Button variant="secondary" size="md">Create account</Button></Link>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-[16px]">
        {[
          { icon: "🐶", title: "Pet Owners", desc: "Register your pets and book appointments with trusted vets." },
          { icon: "🩺", title: "Veterinarians", desc: "Manage your locations, schedule, and document visits." },
          { icon: "📊", title: "Administrators", desc: "Monitor clinic activity and manage user accounts." },
        ].map(({ icon, title, desc }) => (
          <Card key={title} className="text-center">
            <div className="text-[32px] mb-[10px]">{icon}</div>
            <h3 className="m-0 mb-[6px] text-[15px] font-bold">{title}</h3>
            <p className="m-0 text-[13px] text-muted leading-[1.5]">{desc}</p>
          </Card>
        ))}
      </div>
    </PageLayout>
  );
}

export default HomePage;
