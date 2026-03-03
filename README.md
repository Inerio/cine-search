# MovieSeeker

A full-stack movie and TV show discovery application with AI-powered natural language search. Describe a scene, name an actor, or type a vague memory — MovieSeeker finds what you're looking for.

**[English](#english)** | **[Francais](#francais)**

---

<a id="english"></a>

## Features

- **Smart Search** — Search movies and TV shows by title, actor, director, or genre with advanced filtering (year, language, rating, duration, sort order)
- **AI-Powered Search** — Describe what you're looking for in plain language (e.g. *"a dark sci-fi movie where someone explores space and time"*) and let the AI identify it with a confidence level, suggestions, and similar titles
- **Explore** — Browse trending, popular movies and TV shows with rich filtering
- **Actor & Director Pages** — Browse filmographies with filters (gender, genre, country, sort)
- **Movie & TV Details** — Synopsis, cast, ratings, and where to watch (streaming, rent, buy) via JustWatch data
- **Bilingual UI** — Full English and French interface with instant language switching
- **Responsive** — Optimized for desktop, tablet, and mobile

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 21, Spring Boot 3.5, Spring WebFlux |
| **Frontend** | Angular 19, TypeScript 5.7, SCSS |
| **AI** | Groq API (Llama 3.3 70B) |
| **Data** | TMDB API |
| **Cache** | Caffeine (in-memory, 10 min TTL) |
| **Testing** | JUnit 5 + Mockito (backend), Jest (frontend), ESLint |
| **CI/CD** | GitHub Actions |
| **Deployment** | Docker Compose, Nginx |

## Architecture

```
Browser  -->  Nginx (port 80/4200)
                |
                +--> /api/*  -->  Spring Boot (port 8080)
                |                     |
                |                     +--> TMDB API (movies, tv, persons)
                |                     +--> Groq API (LLM parsing)
                |
                +--> /*  -->  Angular SPA (static files)
```

## Getting Started

### Prerequisites

- **Java 21** (JDK)
- **Node.js 22** + npm
- A [TMDB API key](https://www.themoviedb.org/settings/api) (free)
- A [Groq API key](https://console.groq.com/keys) (free tier available)

### Local Development

**1. Clone the repository**

```bash
git clone https://github.com/your-username/cine-search.git
cd cine-search
```

**2. Backend**

```bash
cd cine-search-back
cp ../. env.example ../.env   # then fill in your API keys
export TMDB_API_KEY=your_key
export GROQ_API_KEY=your_key
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

**3. Frontend**

```bash
cd cine-search-front
npm install
npm start
```

The app opens at `http://localhost:4200`. API calls are proxied to `:8080` automatically.

### Docker (Production)

```bash
# Create .env at the project root
cp .env.example .env
# Edit .env with your TMDB_API_KEY and GROQ_API_KEY

docker compose up -d --build
```

Frontend: `http://localhost:4200` — Backend API: `http://localhost:8080`

## Testing

```bash
# Backend (99 tests)
cd cine-search-back && ./mvnw verify

# Frontend (48 tests + lint)
cd cine-search-front && npm test && npx ng lint
```

## Project Structure

```
cine-search/
  docker-compose.yml
  .env.example
  .github/workflows/         # CI + deploy pipelines
  cine-search-back/          # Spring Boot API
    src/main/java/com/cinesearch/
      controller/             # REST endpoints (Movie, TV, Person, AI)
      service/                # TMDB + Groq integrations
      config/                 # CORS, cache, error handling
      dto/                    # Data transfer objects
    src/test/                 # JUnit 5 + Mockito tests
  cine-search-front/         # Angular SPA
    src/app/
      components/             # UI components (search, detail, cards...)
      services/               # HTTP client, i18n, image helpers
      models/                 # TypeScript interfaces
      utils/                  # Shared constants, filters, pagination
      i18n/                   # EN + FR translations
```

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `TMDB_API_KEY` | Yes | The Movie Database API key |
| `GROQ_API_KEY` | Yes | Groq LLM API key |
| `CORS_ALLOWED_ORIGINS` | No | Allowed origins (default: `http://localhost:4200`) |
| `LLM_MODEL` | No | Groq model (default: `llama-3.3-70b-versatile`) |
| `LLM_TEMPERATURE` | No | LLM temperature (default: `0.1`) |
| `LLM_MAX_TOKENS` | No | Max output tokens (default: `1024`) |

## License

This project is licensed under the [MIT License](LICENSE).

---

<a id="francais"></a>

## Fonctionnalites

- **Recherche intelligente** — Recherchez des films et series par titre, acteur, realisateur ou genre avec filtres avances (annee, langue, note, duree, tri)
- **Recherche IA** — Decrivez ce que vous cherchez en langage naturel (ex: *"un film de science-fiction sombre ou quelqu'un explore l'espace et le temps"*) et laissez l'IA l'identifier avec un niveau de confiance, des suggestions et des titres similaires
- **Explorer** — Parcourez les films et series tendance et populaires avec des filtres riches
- **Pages Acteur & Realisateur** — Filmographies avec filtres (genre, pays, tri)
- **Details Film & Serie** — Synopsis, casting, notes et ou regarder (streaming, location, achat) via JustWatch
- **Interface bilingue** — Interface complete en anglais et francais avec changement instantane
- **Responsive** — Optimise pour desktop, tablette et mobile

## Stack Technique

| Couche | Technologie |
|--------|-------------|
| **Backend** | Java 21, Spring Boot 3.5, Spring WebFlux |
| **Frontend** | Angular 19, TypeScript 5.7, SCSS |
| **IA** | Groq API (Llama 3.3 70B) |
| **Donnees** | API TMDB |
| **Cache** | Caffeine (en memoire, TTL 10 min) |
| **Tests** | JUnit 5 + Mockito (backend), Jest (frontend), ESLint |
| **CI/CD** | GitHub Actions |
| **Deploiement** | Docker Compose, Nginx |

## Demarrage Rapide

### Prerequis

- **Java 21** (JDK)
- **Node.js 22** + npm
- Une [cle API TMDB](https://www.themoviedb.org/settings/api) (gratuite)
- Une [cle API Groq](https://console.groq.com/keys) (offre gratuite disponible)

### Developpement Local

**1. Cloner le depot**

```bash
git clone https://github.com/your-username/cine-search.git
cd cine-search
```

**2. Backend**

```bash
cd cine-search-back
export TMDB_API_KEY=votre_cle
export GROQ_API_KEY=votre_cle
./mvnw spring-boot:run
```

L'API demarre sur `http://localhost:8080`.

**3. Frontend**

```bash
cd cine-search-front
npm install
npm start
```

L'application s'ouvre sur `http://localhost:4200`. Les appels API sont proxies vers `:8080` automatiquement.

### Docker (Production)

```bash
# Creer le .env a la racine du projet
cp .env.example .env
# Editez .env avec vos cles TMDB_API_KEY et GROQ_API_KEY

docker compose up -d --build
```

Frontend : `http://localhost:4200` — API Backend : `http://localhost:8080`

## Tests

```bash
# Backend (99 tests)
cd cine-search-back && ./mvnw verify

# Frontend (48 tests + lint)
cd cine-search-front && npm test && npx ng lint
```

## Variables d'Environnement

| Variable | Requise | Description |
|----------|---------|-------------|
| `TMDB_API_KEY` | Oui | Cle API The Movie Database |
| `GROQ_API_KEY` | Oui | Cle API Groq LLM |
| `CORS_ALLOWED_ORIGINS` | Non | Origines autorisees (defaut : `http://localhost:4200`) |
| `LLM_MODEL` | Non | Modele Groq (defaut : `llama-3.3-70b-versatile`) |
| `LLM_TEMPERATURE` | Non | Temperature LLM (defaut : `0.1`) |
| `LLM_MAX_TOKENS` | Non | Tokens de sortie max (defaut : `1024`) |

## Licence

Ce projet est sous licence [MIT](LICENSE).
