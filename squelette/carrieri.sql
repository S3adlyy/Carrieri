-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : lun. 09 fév. 2026 à 14:28
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `carrieri`
--

-- --------------------------------------------------------

--
-- Structure de la table `artifact`
--

CREATE TABLE `artifact` (
  `id` int(11) NOT NULL,
  `artifact_name` varchar(255) NOT NULL,
  `artifact_description` longtext NOT NULL,
  `artifact_type` varchar(255) NOT NULL,
  `language` varchar(255) DEFAULT NULL,
  `test_content` longtext DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `track_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `candidat_certification`
--

CREATE TABLE `candidat_certification` (
  `certification_id` int(11) NOT NULL,
  `candidat_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `candidat_mission`
--

CREATE TABLE `candidat_mission` (
  `candidat_id` int(11) NOT NULL,
  `mission_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `certification`
--

CREATE TABLE `certification` (
  `id` int(11) NOT NULL,
  `titre` varchar(255) NOT NULL,
  `description` longtext NOT NULL,
  `date_obtention` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `conversation`
--

CREATE TABLE `conversation` (
  `id` int(11) NOT NULL,
  `date_creation` datetime NOT NULL,
  `dernier_message` varchar(255) DEFAULT NULL,
  `statut` varchar(255) NOT NULL,
  `user1_id` int(11) DEFAULT NULL,
  `user2_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `cours`
--

CREATE TABLE `cours` (
  `id` int(11) NOT NULL,
  `titre` varchar(255) NOT NULL,
  `description` longtext NOT NULL,
  `duree` int(11) NOT NULL,
  `niveau` varchar(255) NOT NULL,
  `competences_visees` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`competences_visees`)),
  `est_obligatoire` tinyint(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `cours_mission`
--

CREATE TABLE `cours_mission` (
  `cours_id` int(11) NOT NULL,
  `mission_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `doctrine_migration_versions`
--

CREATE TABLE `doctrine_migration_versions` (
  `version` varchar(191) NOT NULL,
  `executed_at` datetime DEFAULT NULL,
  `execution_time` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `doctrine_migration_versions`
--

INSERT INTO `doctrine_migration_versions` (`version`, `executed_at`, `execution_time`) VALUES
('DoctrineMigrations\\Version20260206153902', '2026-02-06 15:39:33', 971);

-- --------------------------------------------------------

--
-- Structure de la table `entretien`
--

CREATE TABLE `entretien` (
  `id` int(11) NOT NULL,
  `date_entretien` datetime NOT NULL,
  `type` varchar(255) NOT NULL,
  `status` varchar(255) NOT NULL,
  `postulation_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `feedback`
--

CREATE TABLE `feedback` (
  `id` int(11) NOT NULL,
  `commentaire` longtext NOT NULL,
  `note` int(11) NOT NULL,
  `created_at` datetime NOT NULL,
  `rendu_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `file_object`
--

CREATE TABLE `file_object` (
  `id` int(11) NOT NULL,
  `storage_key` varchar(255) NOT NULL,
  `public_url` varchar(255) NOT NULL,
  `mime_type` varchar(255) NOT NULL,
  `file_size` int(11) NOT NULL,
  `uploaded_at` datetime NOT NULL,
  `artifact_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `message`
--

CREATE TABLE `message` (
  `id` int(11) NOT NULL,
  `contenu` longtext NOT NULL,
  `date_envoi` datetime NOT NULL,
  `date_modification` datetime DEFAULT NULL,
  `statut` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `conversation_id` int(11) DEFAULT NULL,
  `expediteur_id` int(11) DEFAULT NULL,
  `destinataire_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `messenger_messages`
--

CREATE TABLE `messenger_messages` (
  `id` bigint(20) NOT NULL,
  `body` longtext NOT NULL,
  `headers` longtext NOT NULL,
  `queue_name` varchar(190) NOT NULL,
  `created_at` datetime NOT NULL,
  `available_at` datetime NOT NULL,
  `delivered_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `mission`
--

CREATE TABLE `mission` (
  `id` int(11) NOT NULL,
  `description` longtext NOT NULL,
  `score_min` int(11) NOT NULL,
  `type` varchar(50) DEFAULT 'ADDITION',
  `created_at` datetime NOT NULL,
  `created_by_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `mission`
--

INSERT INTO `mission` (`id`, `description`, `score_min`, `type`, `created_at`, `created_by_id`) VALUES
(5, 'Design database schema', 70, 'ADDITION', '2026-02-06 19:53:22', 1),
(6, 'Implement CRUD operations', 90, 'ADDITION', '2026-02-06 19:53:22', 1),
(7, 'Test application functionalityyyyyy', 60, 'ADDITION', '2026-02-06 19:53:22', 1),
(11, 'hello ', 60, 'ADDITION', '2026-02-07 15:12:57', 1),
(16, 'wawa', 60, 'ADDITION', '2026-02-08 17:43:05', 1),
(19, 'testtte', 60, 'ADDITION', '2026-02-09 01:07:17', 1);

-- --------------------------------------------------------

--
-- Structure de la table `offre_emploi`
--

CREATE TABLE `offre_emploi` (
  `id` int(11) NOT NULL,
  `titre` varchar(255) NOT NULL,
  `description` longtext NOT NULL,
  `salaire` double NOT NULL,
  `type_contrat` varchar(255) NOT NULL,
  `localisation` varchar(255) NOT NULL,
  `date_publication` datetime NOT NULL,
  `date_expiration` datetime NOT NULL,
  `niveau_qualification` varchar(255) NOT NULL,
  `experience_requise` int(11) NOT NULL,
  `competences_requises` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`competences_requises`)),
  `secteur_activite` varchar(255) NOT NULL,
  `entreprise` varchar(255) NOT NULL,
  `contact_recruteur` varchar(255) NOT NULL,
  `recruteur_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `postulation`
--

CREATE TABLE `postulation` (
  `id` int(11) NOT NULL,
  `date_postulation` datetime NOT NULL,
  `statut` varchar(255) NOT NULL,
  `motivation_candidature` longtext NOT NULL,
  `candidat_id` int(11) DEFAULT NULL,
  `offre_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `reclamation`
--

CREATE TABLE `reclamation` (
  `id` int(11) NOT NULL,
  `objet` varchar(255) NOT NULL,
  `description` longtext NOT NULL,
  `categorie` varchar(255) NOT NULL,
  `date_creation` datetime NOT NULL,
  `statut` varchar(255) NOT NULL,
  `priorite` varchar(255) NOT NULL,
  `utilisateur_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `rendu_mission`
--

CREATE TABLE `rendu_mission` (
  `id` int(11) NOT NULL,
  `code_solution` text DEFAULT NULL,
  `fichier` varchar(255) DEFAULT '',
  `date_rendu` datetime NOT NULL,
  `score` double DEFAULT NULL,
  `resultat` varchar(255) DEFAULT NULL,
  `mission_id` int(11) NOT NULL,
  `candidat_id` int(11) NOT NULL,
  `feedback` text DEFAULT NULL,
  `langue` varchar(20) DEFAULT 'python'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `rendu_mission`
--

INSERT INTO `rendu_mission` (`id`, `code_solution`, `fichier`, `date_rendu`, `score`, `resultat`, `mission_id`, `candidat_id`, `feedback`, `langue`) VALUES
(6, 'print(\"hello world\")', '', '2026-02-07 00:00:00', 5, 'REJECTED', 6, 1, 'Échec complet.', 'python'),
(7, '# This should score low (20-40%)\na = int(input())\nb = int(input())\nprint(a * b)  # Multiply instead of add', '', '2026-02-07 00:00:00', 5, 'REJECTED', 5, 1, 'Échec complet.', 'python'),
(10, 'def add(a, b)\n    return a + b  # MISSING COLON = SYNTAX ERROR\n\nif __name__ == \"__main__\":\n    x = int(input())\n    y = int(input())\n    print(add(x, y))', '', '2026-02-07 00:00:00', 5, 'REJECTED', 7, 1, 'Échec complet.', 'python'),
(13, 'a = int(input())\nb = int(input())\nprint(a + b)', '', '2026-02-07 00:00:00', 5, 'REJECTED', 7, 1, 'Échec complet.', 'python'),
(20, 'def main():\n    data = sys.stdin.read().strip().split()\n    if len(data) >= 2:\n        a = int(data[0])\n        b = int(data[1])\n        print(a + b)\n\nif __name__ == \"__main__\":\n    main()', '', '2026-02-07 00:00:00', 20, 'REJECTED', 7, 1, 'Code faible.', 'python'),
(21, 'a = int(input())\nb = int(input())\nprint(a + b)', '', '2026-02-07 00:00:00', 45, 'REJECTED', 7, 1, 'Code médiocre.', 'python'),
(26, 'adddaffd', '', '2026-02-08 00:00:00', 85, 'ERROR', 7, 1, 'Erreur: Connect to 127.0.0.1:5000 [/127.0.0.1] failed: Connection refused: connect', 'python'),
(29, 'hello world ', '', '2026-02-08 00:00:00', 5, 'REJECTED', 6, 1, 'Échec complet.', 'python'),
(30, 'sdsdsdsdsdsds', '', '2026-02-08 00:00:00', 5, 'REJECTED', 5, 1, 'Échec complet.', 'python'),
(32, 'sdsdsds', '', '2026-02-08 00:00:00', 5, 'REJECTED', 5, 1, 'Échec complet.', 'python'),
(35, '1+2=3', '', '2026-02-08 00:00:00', 5, 'REJECTED', 5, 1, 'Échec complet.', 'python'),
(36, 'z+2', '', '2026-02-08 00:00:00', 5, 'REJECTED', 16, 1, 'Échec complet.', 'python'),
(37, 'a=kkmk', '', '2026-02-08 00:00:00', 5, 'REJECTED', 7, 1, 'Échec complet.', 'python'),
(38, '1+1', '', '2026-02-08 00:00:00', 5, 'REJECTED', 5, 1, 'Échec complet.', 'python'),
(39, 'sdsdsds', '', '2026-02-09 00:00:00', 5, 'REJECTED', 19, 1, 'Échec complet.', 'python');

-- --------------------------------------------------------

--
-- Structure de la table `snapshot`
--

CREATE TABLE `snapshot` (
  `id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `message` longtext NOT NULL,
  `is_final` tinyint(4) NOT NULL,
  `created_at` datetime NOT NULL,
  `track_id` int(11) DEFAULT NULL,
  `author_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `snapshot_item`
--

CREATE TABLE `snapshot_item` (
  `snapshot_id` int(11) NOT NULL,
  `artifact_id` int(11) NOT NULL,
  `file_object_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `track`
--

CREATE TABLE `track` (
  `id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` longtext NOT NULL,
  `category` varchar(255) NOT NULL,
  `start_date` datetime NOT NULL,
  `end_date` datetime DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `created_at` datetime NOT NULL,
  `visibility` varchar(255) NOT NULL,
  `workspace_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `traitement_reclamation`
--

CREATE TABLE `traitement_reclamation` (
  `id` int(11) NOT NULL,
  `date_traitement` datetime NOT NULL,
  `reponse_admin` longtext NOT NULL,
  `statut_final` varchar(255) NOT NULL,
  `reclamation_id` int(11) DEFAULT NULL,
  `admin_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `user`
--

CREATE TABLE `user` (
  `id` int(11) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `roles` varchar(255) NOT NULL,
  `is_active` tinyint(4) NOT NULL,
  `last_login_at` date NOT NULL,
  `created_at` date NOT NULL,
  `type` varchar(255) NOT NULL,
  `headline` varchar(255) DEFAULT NULL,
  `bio` longtext DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `visibility` varchar(255) DEFAULT NULL,
  `niveau` varchar(255) DEFAULT NULL,
  `score_global` double DEFAULT NULL,
  `org_name` varchar(255) DEFAULT NULL,
  `description` longtext DEFAULT NULL,
  `website_url` varchar(255) DEFAULT NULL,
  `logo_url` varchar(255) DEFAULT NULL,
  `profile_pic` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `user`
--

INSERT INTO `user` (`id`, `first_name`, `last_name`, `email`, `password_hash`, `roles`, `is_active`, `last_login_at`, `created_at`, `type`, `headline`, `bio`, `location`, `visibility`, `niveau`, `score_global`, `org_name`, `description`, `website_url`, `logo_url`, `profile_pic`) VALUES
(1, 'Admin', 'User', 'admin@carrieri.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '[\"ROLE_ADMIN\"]', 1, '0000-00-00', '2026-02-06', 'admin', 'System Administrator', 'System admin account', 'Headquarters', 'public', 'expert', 100, 'Carrieri Corp', 'System administration account', 'https://carrieri.com', 'https://carrieri.com/logo.png', 'https://carrieri.com/profile.png');

-- --------------------------------------------------------

--
-- Structure de la table `workspace`
--

CREATE TABLE `workspace` (
  `id` int(11) NOT NULL,
  `description` longtext NOT NULL,
  `created_at` datetime NOT NULL,
  `candidat_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `artifact`
--
ALTER TABLE `artifact`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_48E5602C5ED23C43` (`track_id`);

--
-- Index pour la table `candidat_certification`
--
ALTER TABLE `candidat_certification`
  ADD PRIMARY KEY (`certification_id`,`candidat_id`),
  ADD KEY `IDX_8D903D8ECB47068A` (`certification_id`),
  ADD KEY `IDX_8D903D8E8D0EB82` (`candidat_id`);

--
-- Index pour la table `candidat_mission`
--
ALTER TABLE `candidat_mission`
  ADD PRIMARY KEY (`candidat_id`,`mission_id`),
  ADD KEY `IDX_C96A04D68D0EB82` (`candidat_id`),
  ADD KEY `IDX_C96A04D6BE6CAE90` (`mission_id`);

--
-- Index pour la table `certification`
--
ALTER TABLE `certification`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `conversation`
--
ALTER TABLE `conversation`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_8A8E26E956AE248B` (`user1_id`),
  ADD KEY `IDX_8A8E26E9441B8B65` (`user2_id`);

--
-- Index pour la table `cours`
--
ALTER TABLE `cours`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `cours_mission`
--
ALTER TABLE `cours_mission`
  ADD PRIMARY KEY (`cours_id`,`mission_id`),
  ADD KEY `IDX_D3619B477ECF78B0` (`cours_id`),
  ADD KEY `IDX_D3619B47BE6CAE90` (`mission_id`);

--
-- Index pour la table `doctrine_migration_versions`
--
ALTER TABLE `doctrine_migration_versions`
  ADD PRIMARY KEY (`version`);

--
-- Index pour la table `entretien`
--
ALTER TABLE `entretien`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_2B58D6DAD749FDF1` (`postulation_id`);

--
-- Index pour la table `feedback`
--
ALTER TABLE `feedback`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UNIQ_D2294458C974D9ED` (`rendu_id`);

--
-- Index pour la table `file_object`
--
ALTER TABLE `file_object`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_10BA8D53E28B07AC` (`artifact_id`);

--
-- Index pour la table `message`
--
ALTER TABLE `message`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_B6BD307F9AC0396` (`conversation_id`),
  ADD KEY `IDX_B6BD307F10335F61` (`expediteur_id`),
  ADD KEY `IDX_B6BD307FA4F84F6E` (`destinataire_id`);

--
-- Index pour la table `messenger_messages`
--
ALTER TABLE `messenger_messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_75EA56E0FB7336F0E3BD61CE16BA31DBBF396750` (`queue_name`,`available_at`,`delivered_at`,`id`);

--
-- Index pour la table `mission`
--
ALTER TABLE `mission`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_9067F23CB03A8386` (`created_by_id`);

--
-- Index pour la table `offre_emploi`
--
ALTER TABLE `offre_emploi`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_132AD0D1BB0859F1` (`recruteur_id`);

--
-- Index pour la table `postulation`
--
ALTER TABLE `postulation`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_DA7D4E9B8D0EB82` (`candidat_id`),
  ADD KEY `IDX_DA7D4E9B4CC8505A` (`offre_id`);

--
-- Index pour la table `reclamation`
--
ALTER TABLE `reclamation`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_CE606404FB88E14F` (`utilisateur_id`);

--
-- Index pour la table `rendu_mission`
--
ALTER TABLE `rendu_mission`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_84BAC7D3BE6CAE90` (`mission_id`),
  ADD KEY `IDX_84BAC7D38D0EB82` (`candidat_id`);

--
-- Index pour la table `snapshot`
--
ALTER TABLE `snapshot`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_2C4D15355ED23C43` (`track_id`),
  ADD KEY `IDX_2C4D1535F675F31B` (`author_id`);

--
-- Index pour la table `snapshot_item`
--
ALTER TABLE `snapshot_item`
  ADD PRIMARY KEY (`snapshot_id`,`artifact_id`),
  ADD KEY `IDX_D4BB0637B39395E` (`snapshot_id`),
  ADD KEY `IDX_D4BB063E28B07AC` (`artifact_id`),
  ADD KEY `IDX_D4BB063AD22E95D` (`file_object_id`);

--
-- Index pour la table `track`
--
ALTER TABLE `track`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_D6E3F8A682D40A1F` (`workspace_id`);

--
-- Index pour la table `traitement_reclamation`
--
ALTER TABLE `traitement_reclamation`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_FE317EF32D6BA2D9` (`reclamation_id`),
  ADD KEY `IDX_FE317EF3642B8210` (`admin_id`);

--
-- Index pour la table `user`
--
ALTER TABLE `user`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `workspace`
--
ALTER TABLE `workspace`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_8D9400198D0EB82` (`candidat_id`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `artifact`
--
ALTER TABLE `artifact`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `certification`
--
ALTER TABLE `certification`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `conversation`
--
ALTER TABLE `conversation`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `cours`
--
ALTER TABLE `cours`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `entretien`
--
ALTER TABLE `entretien`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `feedback`
--
ALTER TABLE `feedback`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `file_object`
--
ALTER TABLE `file_object`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `message`
--
ALTER TABLE `message`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `messenger_messages`
--
ALTER TABLE `messenger_messages`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `mission`
--
ALTER TABLE `mission`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=20;

--
-- AUTO_INCREMENT pour la table `offre_emploi`
--
ALTER TABLE `offre_emploi`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `postulation`
--
ALTER TABLE `postulation`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `reclamation`
--
ALTER TABLE `reclamation`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `rendu_mission`
--
ALTER TABLE `rendu_mission`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=40;

--
-- AUTO_INCREMENT pour la table `snapshot`
--
ALTER TABLE `snapshot`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `track`
--
ALTER TABLE `track`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `traitement_reclamation`
--
ALTER TABLE `traitement_reclamation`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `user`
--
ALTER TABLE `user`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `workspace`
--
ALTER TABLE `workspace`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `artifact`
--
ALTER TABLE `artifact`
  ADD CONSTRAINT `FK_48E5602C5ED23C43` FOREIGN KEY (`track_id`) REFERENCES `track` (`id`);

--
-- Contraintes pour la table `candidat_certification`
--
ALTER TABLE `candidat_certification`
  ADD CONSTRAINT `FK_8D903D8E8D0EB82` FOREIGN KEY (`candidat_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_8D903D8ECB47068A` FOREIGN KEY (`certification_id`) REFERENCES `certification` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `candidat_mission`
--
ALTER TABLE `candidat_mission`
  ADD CONSTRAINT `FK_C96A04D68D0EB82` FOREIGN KEY (`candidat_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_C96A04D6BE6CAE90` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `conversation`
--
ALTER TABLE `conversation`
  ADD CONSTRAINT `FK_8A8E26E9441B8B65` FOREIGN KEY (`user2_id`) REFERENCES `user` (`id`),
  ADD CONSTRAINT `FK_8A8E26E956AE248B` FOREIGN KEY (`user1_id`) REFERENCES `user` (`id`);

--
-- Contraintes pour la table `cours_mission`
--
ALTER TABLE `cours_mission`
  ADD CONSTRAINT `FK_D3619B477ECF78B0` FOREIGN KEY (`cours_id`) REFERENCES `cours` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_D3619B47BE6CAE90` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `entretien`
--
ALTER TABLE `entretien`
  ADD CONSTRAINT `FK_2B58D6DAD749FDF1` FOREIGN KEY (`postulation_id`) REFERENCES `postulation` (`id`);

--
-- Contraintes pour la table `feedback`
--
ALTER TABLE `feedback`
  ADD CONSTRAINT `FK_D2294458C974D9ED` FOREIGN KEY (`rendu_id`) REFERENCES `rendu_mission` (`id`);

--
-- Contraintes pour la table `file_object`
--
ALTER TABLE `file_object`
  ADD CONSTRAINT `FK_10BA8D53E28B07AC` FOREIGN KEY (`artifact_id`) REFERENCES `artifact` (`id`);

--
-- Contraintes pour la table `message`
--
ALTER TABLE `message`
  ADD CONSTRAINT `FK_B6BD307F10335F61` FOREIGN KEY (`expediteur_id`) REFERENCES `user` (`id`),
  ADD CONSTRAINT `FK_B6BD307F9AC0396` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`),
  ADD CONSTRAINT `FK_B6BD307FA4F84F6E` FOREIGN KEY (`destinataire_id`) REFERENCES `user` (`id`);

--
-- Contraintes pour la table `mission`
--
ALTER TABLE `mission`
  ADD CONSTRAINT `FK_9067F23CB03A8386` FOREIGN KEY (`created_by_id`) REFERENCES `user` (`id`);

--
-- Contraintes pour la table `offre_emploi`
--
ALTER TABLE `offre_emploi`
  ADD CONSTRAINT `FK_132AD0D1BB0859F1` FOREIGN KEY (`recruteur_id`) REFERENCES `user` (`id`);

--
-- Contraintes pour la table `postulation`
--
ALTER TABLE `postulation`
  ADD CONSTRAINT `FK_DA7D4E9B4CC8505A` FOREIGN KEY (`offre_id`) REFERENCES `offre_emploi` (`id`),
  ADD CONSTRAINT `FK_DA7D4E9B8D0EB82` FOREIGN KEY (`candidat_id`) REFERENCES `user` (`id`);

--
-- Contraintes pour la table `reclamation`
--
ALTER TABLE `reclamation`
  ADD CONSTRAINT `FK_CE606404FB88E14F` FOREIGN KEY (`utilisateur_id`) REFERENCES `user` (`id`);

--
-- Contraintes pour la table `rendu_mission`
--
ALTER TABLE `rendu_mission`
  ADD CONSTRAINT `FK_84BAC7D38D0EB82` FOREIGN KEY (`candidat_id`) REFERENCES `user` (`id`),
  ADD CONSTRAINT `FK_84BAC7D3BE6CAE90` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`);

--
-- Contraintes pour la table `snapshot`
--
ALTER TABLE `snapshot`
  ADD CONSTRAINT `FK_2C4D15355ED23C43` FOREIGN KEY (`track_id`) REFERENCES `track` (`id`),
  ADD CONSTRAINT `FK_2C4D1535F675F31B` FOREIGN KEY (`author_id`) REFERENCES `user` (`id`);

--
-- Contraintes pour la table `snapshot_item`
--
ALTER TABLE `snapshot_item`
  ADD CONSTRAINT `FK_D4BB0637B39395E` FOREIGN KEY (`snapshot_id`) REFERENCES `snapshot` (`id`),
  ADD CONSTRAINT `FK_D4BB063AD22E95D` FOREIGN KEY (`file_object_id`) REFERENCES `file_object` (`id`),
  ADD CONSTRAINT `FK_D4BB063E28B07AC` FOREIGN KEY (`artifact_id`) REFERENCES `artifact` (`id`);

--
-- Contraintes pour la table `track`
--
ALTER TABLE `track`
  ADD CONSTRAINT `FK_D6E3F8A682D40A1F` FOREIGN KEY (`workspace_id`) REFERENCES `workspace` (`id`);

--
-- Contraintes pour la table `traitement_reclamation`
--
ALTER TABLE `traitement_reclamation`
  ADD CONSTRAINT `FK_FE317EF32D6BA2D9` FOREIGN KEY (`reclamation_id`) REFERENCES `reclamation` (`id`),
  ADD CONSTRAINT `FK_FE317EF3642B8210` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`);

--
-- Contraintes pour la table `workspace`
--
ALTER TABLE `workspace`
  ADD CONSTRAINT `FK_8D9400198D0EB82` FOREIGN KEY (`candidat_id`) REFERENCES `user` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
