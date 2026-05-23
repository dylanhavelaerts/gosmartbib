import { useAuth } from "../../context/AuthContext";
import "./UserInfoWidget.css";

export default function UserInfoWidget({
  user,
}: {
  user: ReturnType<typeof useAuth>["user"];
}) {
  const cls = user?.classes?.[0];

  return (
    <div className="widget widget--user-info">
      <p className="widget-label">Profiel</p>
      <div className="user-avatar">
        <img src="/user.png" alt="Gebruiker" className="user-avatar-icon" />
      </div>
      <p className="user-name">{user?.smartschoolUid ?? "—"}</p>
      {cls && (
        <div className="user-tags">
          {cls.name && <span className="user-tag">{cls.name}</span>}
          {cls.grade && <span className="user-tag">{cls.grade}</span>}
          {cls.schoolYear && <span className="user-tag">{cls.schoolYear}</span>}
        </div>
      )}
    </div>
  );
}
