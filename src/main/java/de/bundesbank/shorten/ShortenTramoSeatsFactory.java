/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.bundesbank.shorten;

import ec.nbdemetra.sa.DiagnosticsMatrixView;
import ec.satoolkit.tramoseats.TramoSeatsSpecification;
import ec.tss.sa.documents.TramoSeatsDocument;
import ec.tstoolkit.algorithm.CompositeResults;
import ec.tstoolkit.algorithm.IProcessing;
import ec.tstoolkit.modelling.arima.tramo.EstimateSpec;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.utilities.DefaultInformationExtractor;
import ec.tstoolkit.utilities.Id;
import ec.tstoolkit.utilities.LinearId;
import ec.ui.view.tsprocessing.ComposedProcDocumentItemFactory;
import ec.ui.view.tsprocessing.DefaultItemUI;
import ec.ui.view.tsprocessing.IProcDocumentView;
import ec.ui.view.tsprocessing.ProcDocumentItemFactory;
import static ec.ui.view.tsprocessing.sa.SaDocumentViewFactory.DIAGNOSTICS;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JComponent;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Thomas Witthohn
 */
@ServiceProvider(service = ProcDocumentItemFactory.class, position = 600050)
public class ShortenTramoSeatsFactory extends ComposedProcDocumentItemFactory<TramoSeatsDocument, Map<String, CompositeResults>> {

    private static final String SHORT = "Short Model Stability";
    private static final Id DIAGNOSTIC_SHORT = new LinearId(DIAGNOSTICS, SHORT);

    public ShortenTramoSeatsFactory() {
        super(TramoSeatsDocument.class, DIAGNOSTIC_SHORT, new DiagnosticsShortenExtractor(), new DefaultItemUI<IProcDocumentView<TramoSeatsDocument>, Map<String, CompositeResults>>() {
            @Override
            public JComponent getView(IProcDocumentView<TramoSeatsDocument> host, Map<String, CompositeResults> information) {
                return new DiagnosticsMatrixView(information);
            }

        });
    }

    private static class DiagnosticsShortenExtractor extends DefaultInformationExtractor<TramoSeatsDocument, Map<String, CompositeResults>> {

        public static final DiagnosticsShortenExtractor INSTANCE = new DiagnosticsShortenExtractor();

        @Override
        public Map<String, CompositeResults> retrieve(TramoSeatsDocument source) {
            if (source.getInput() == null) {
                return null;
            }
            int frequencyIntValue = source.getInput().getTsData().getFrequency().intValue();

            Map<String, CompositeResults> results = new LinkedHashMap<>();
            TramoSeatsSpecification currentSpec = source.getSpecification();

            int firstyears = 3;
            while (firstyears <= 10) {
                TramoSeatsSpecification shortSpec = currentSpec.clone();
                EstimateSpec estimateSpec = shortSpec.getTramoSpecification().getEstimate();
                estimateSpec.getSpan().first(firstyears * frequencyIntValue);

                IProcessing<TsData, CompositeResults> proc = source.getProcessor().generateProcessing(shortSpec, source.getContext());
                CompositeResults rslt = proc.process(source.getSeries());

                if (rslt != null) {
                    results.put("First " + firstyears + " years", rslt);
                }
                firstyears++;
            }

            results.put("Current spec", source.getResults());

            int lastyears = 10;
            while (lastyears >= 3) {
                TramoSeatsSpecification shortSpec = currentSpec.clone();
                EstimateSpec estimateSpec = shortSpec.getTramoSpecification().getEstimate();
                estimateSpec.getSpan().last(lastyears * frequencyIntValue);

                IProcessing<TsData, CompositeResults> proc = source.getProcessor().generateProcessing(shortSpec, source.getContext());
                CompositeResults rslt = proc.process(source.getSeries());

                if (rslt != null) {
                    results.put("Last " + lastyears + " years", rslt);
                }
                lastyears--;
            }
            return results;
        }
    }

}
