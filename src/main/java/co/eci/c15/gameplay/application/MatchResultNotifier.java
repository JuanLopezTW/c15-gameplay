package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.MatchResult;

public interface MatchResultNotifier {
    void notify(MatchResult result);
}
