package hollowshift.scenes;

import hollowshift.engine.Action;
import hollowshift.engine.Input;
import hollowshift.engine.Lang;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Every note picked up so far, in story order, to read again. Left and right turn the pages. */
final class Journal {
    private final List<Integer> pages;
    private final NotePaper paper;
    private int page;

    Journal(Set<Integer> notes, NotePaper paper) {
        this.pages = new ArrayList<>(notes);
        this.pages.sort(null);
        this.paper = paper;
        page = pages.size() - 1;
    }

    boolean empty() {
        return pages.isEmpty();
    }

    /** False once the journal is closed. */
    boolean update(Input in) {
        if (in.pressed(Action.JOURNAL) || in.pressed(Action.BACK) || in.pressed(Action.INTERACT)) return false;
        if ((in.pressed(Action.LEFT) || in.keyPressed(KeyEvent.VK_LEFT)) && page > 0) page--;
        if ((in.pressed(Action.RIGHT) || in.keyPressed(KeyEvent.VK_RIGHT)) && page < pages.size() - 1) page++;
        return true;
    }

    void render(Graphics2D g, Input in) {
        int n = pages.get(page);
        String hint = Lang.t("journal.hint", page + 1, pages.size(), Input.keyName(in.binding(Action.JOURNAL)[0]));
        paper.render(g, n, Lang.t("note." + n), hint);
    }
}
