package de.vogel612.helper;

import de.vogel612.helper.data.Side;
import de.vogel612.helper.ui.*;
import de.vogel612.helper.data.OverviewModel;

import java.nio.file.Paths;

public class Main {
    // protected for testing verifications
    static final String ARGUMENT_MISMATCH = "Arguments do not match up. Please give the following combination of parameters: {{Path To ResxFile} {locale_left locale_right}}";

    private Main() {
    }

    public static void main(final String[] args) {
        // parsing the first argument given into a proper path to load the resx
        // from
        if (args.length > 1 && args.length != 3) {
            // don't even bother!
            System.out.println(ARGUMENT_MISMATCH);
            return;
        }

        TranslationPresenter tp = new TranslationPresenter();
        ResxChooser rc = new ResxChooser();
        OverviewModel m = new OverviewModel();
        OverviewView v = new SwingOverviewView();

        OverviewPresenter p = new OverviewPresenter(m, v, tp, rc);
        p.show();

        if (args.length == 0) {
            // filechooser
            p.fileChoosing();
        } else {
            // FIXME If there's no supplied path, just open a JFileChooser or somesuch
            p.loadFiles(Paths.get(args[0]).normalize());
            // set the selected locales if they were specified on commandline
            // check whether they are available before that and fall back if they aren't
            if (args.length == 3) {
                final String leftLocale = args[1];
                final String rightLocale = args[2];
                if (m.getAvailableLocales().contains(leftLocale)
                  && m.getAvailableLocales().contains(rightLocale)) {
                    p.onLocaleRequest(leftLocale, Side.LEFT);
                    p.onLocaleRequest(rightLocale, Side.RIGHT);
                }
                // "fallback"
            }
        }
    }

}
