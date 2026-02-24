// --- Movie ---

export interface Movie {
  id: number;
  title: string;
  overview: string;
  poster_path: string | null;
  backdrop_path: string | null;
  release_date: string;
  vote_average: number;
  vote_count: number;
  popularity: number;
  genre_ids: number[];
  original_language: string;
}

export interface MovieListResponse {
  page: number;
  results: Movie[];
  total_pages: number;
  total_results: number;
}

export interface MovieDetail {
  id: number;
  title: string;
  overview: string;
  poster_path: string | null;
  backdrop_path: string | null;
  release_date: string;
  vote_average: number;
  vote_count: number;
  runtime: number;
  tagline: string;
  budget: number;
  revenue: number;
  status: string;
  genres: Genre[];
  credits: Credits;
}

// --- Genre ---

export interface Genre {
  id: number;
  name: string;
}

export interface GenreListResponse {
  genres: Genre[];
}

// --- Credits ---

export interface Credits {
  cast: CastMember[];
  crew: CrewMember[];
}

export interface CastMember {
  id: number;
  name: string;
  character: string;
  profile_path: string | null;
  order: number;
}

export interface CrewMember {
  id: number;
  name: string;
  job: string;
  department: string;
  profile_path: string | null;
}

// --- Person ---

export interface Person {
  id: number;
  name: string;
  profile_path: string | null;
  known_for_department: string;
  popularity: number;
  gender: number; // 0=unknown, 1=female, 2=male, 3=non-binary
  known_for: Movie[];
}

export interface PersonSearchResponse {
  page: number;
  results: Person[];
  total_pages: number;
  total_results: number;
}

export interface PersonCreditsResponse {
  id: number;
  cast: Movie[];
}

// --- AI Parse ---

export interface AiMovieQuery {
  intent: 'search' | 'recommend' | 'details' | 'unknown';
  type: 'movie' | 'tv' | null;
  query: string | null;
  title: string | null;
  year: number | null;
  genres: string[] | null;
  language: string | null;
  country: string | null;
  platform: string | null;
  sort: 'relevance' | 'rating' | 'popularity' | 'recent' | null;
  include_adult: boolean;
}

export interface AiSearchResponse {
  parsed: AiMovieQuery;
  results: Movie[];
  totalResults: number;
}
