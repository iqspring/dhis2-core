package org.hisp.dhis.eventdatavalue;/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class EventDataValueServiceTest extends DhisSpringTest
{
    @Autowired
    private EventDataValueService eventDataValueService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementService dataElementService;

    private ProgramStageInstance stageInstanceA;
    private ProgramStageInstance stageInstanceB;

    private EventDataValue eventDataValueA;
    private EventDataValue eventDataValueB;
    private EventDataValue eventDataValueC;
    private EventDataValue eventDataValueD;
    private EventDataValue eventDataValueE;

    @Override
    public void setUpTest()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );

        TrackedEntityInstance entityInstance = createTrackedEntityInstance( 'A', organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstance );

        Program program = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( program );

        ProgramStage stageA = createProgramStage( 'A', 0 );
        stageA.setProgram( program );
        programStageService.saveProgramStage( stageA );

        ProgramStage stageB = createProgramStage( 'B', 0 );
        stageB.setProgram( program );
        programStageService.saveProgramStage( stageB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        program.setProgramStages( programStages );
        programService.updateProgram( program );

        DateTime yesterDate = DateTime.now();
        yesterDate.withTimeAtStartOfDay();
        yesterDate.minusDays( 1 );
        Date yesterday = yesterDate.toDate();

        ProgramInstance programInstance = programInstanceService.enrollTrackedEntityInstance( entityInstance, program, yesterday,
            yesterday, organisationUnit );

        stageInstanceA = programStageInstanceService.createProgramStageInstance( programInstance, stageA, yesterday,
            yesterday, organisationUnit );
        stageInstanceB = programStageInstanceService.createProgramStageInstance( programInstance, stageB, yesterday,
            yesterday, organisationUnit );

        eventDataValueA = new EventDataValue( dataElementA.getUid(), "1" );
        eventDataValueB = new EventDataValue( dataElementB.getUid(), "2" );
        eventDataValueC = new EventDataValue( dataElementA.getUid(), "3" );
        eventDataValueD = new EventDataValue( dataElementB.getUid(), "4" );
        eventDataValueE = new EventDataValue( dataElementC.getUid(), "5" );
    }

    @Test
    public void testSaveEventDataValue()
    {
        eventDataValueService.saveEventDataValue( stageInstanceA, eventDataValueA );
        eventDataValueService.saveEventDataValues( stageInstanceB, Stream.of(eventDataValueC, eventDataValueD).collect( Collectors.toSet()) );

        ProgramStageInstance psiA = programStageInstanceService.getProgramStageInstance( stageInstanceA.getUid() );
        ProgramStageInstance psiB = programStageInstanceService.getProgramStageInstance( stageInstanceB.getUid() );

        assertEquals( 1, psiA.getEventDataValues().size() );
        assertEquals( 2, psiB.getEventDataValues().size() );

        assertTrue( psiA.getEventDataValues().contains( eventDataValueA ) );
        assertTrue( psiB.getEventDataValues().contains( eventDataValueB ) );
        assertTrue( psiB.getEventDataValues().contains( eventDataValueD ) );
    }

    @Test
    public void testDeleteEventDataValue()
    {
        eventDataValueService.saveEventDataValue( stageInstanceA, eventDataValueA );
        eventDataValueService.saveEventDataValues( stageInstanceB, Stream.of(eventDataValueC, eventDataValueD, eventDataValueE).collect( Collectors.toSet()) );

        ProgramStageInstance psiA = programStageInstanceService.getProgramStageInstance( stageInstanceA.getUid() );
        assertEquals( 1, psiA.getEventDataValues().size() );

        eventDataValueService.deleteEventDataValue( stageInstanceA, eventDataValueA );
        psiA = programStageInstanceService.getProgramStageInstance( stageInstanceA.getUid() );
        assertEquals( 0, psiA.getEventDataValues().size() );


        ProgramStageInstance psiB = programStageInstanceService.getProgramStageInstance( stageInstanceB.getUid() );
        assertEquals( 3, psiB.getEventDataValues().size() );

        eventDataValueService.deleteEventDataValue( stageInstanceB, eventDataValueC );
        psiB = programStageInstanceService.getProgramStageInstance( stageInstanceB.getUid() );
        assertEquals( 2, psiB.getEventDataValues().size() );

        eventDataValueService.deleteEventDataValues( stageInstanceB, Stream.of( eventDataValueD, eventDataValueE ).collect( Collectors.toSet()) );
        psiB = programStageInstanceService.getProgramStageInstance( stageInstanceB.getUid() );
        assertEquals( 0, psiB.getEventDataValues().size() );
    }

    @Test
    public void testUpdateTrackedEntityDataValue()
    {
        eventDataValueService.saveEventDataValue( stageInstanceA, eventDataValueA );
        eventDataValueService.saveEventDataValues( stageInstanceB, Stream.of(eventDataValueC, eventDataValueD, eventDataValueE).collect( Collectors.toSet()) );

        ProgramStageInstance psiA = programStageInstanceService.getProgramStageInstance( stageInstanceA.getUid() );
        assertEquals( eventDataValueA.getValue(), psiA.getEventDataValues().iterator().next().getValue() );

        eventDataValueA.setValue( "2" );
        eventDataValueService.updateEventDataValue( stageInstanceA, eventDataValueA);
        psiA = programStageInstanceService.getProgramStageInstance( stageInstanceA.getUid() );
        assertEquals( eventDataValueA.getValue(), psiA.getEventDataValues().iterator().next().getValue() );

        ProgramStageInstance psiB = programStageInstanceService.getProgramStageInstance( stageInstanceB.getUid() );
        List<EventDataValue> eventDataValues = new ArrayList<>( psiB.getEventDataValues() );
        eventDataValues.sort( Comparator.comparing( EventDataValue::getDataElement ) );

        assertEquals( eventDataValueC.getValue(), eventDataValues.get( 0 ).getValue() );
        assertEquals( eventDataValueD.getValue(), eventDataValues.get( 1 ).getValue() );
        assertEquals( eventDataValueE.getValue(), eventDataValues.get( 2 ).getValue() );

        eventDataValueC.setValue( "42" );
        eventDataValueD.setValue( "15" );
        eventDataValueService.updateEventDataValues( stageInstanceB, Stream.of( eventDataValueC, eventDataValueD ).collect( Collectors.toSet()) );

        psiB = programStageInstanceService.getProgramStageInstance( stageInstanceB.getUid() );
        eventDataValues = new ArrayList<>( psiB.getEventDataValues() );
        eventDataValues.sort( Comparator.comparing( EventDataValue::getDataElement ) );

        assertEquals( eventDataValueC.getValue(), eventDataValues.get( 0 ).getValue() );
        assertEquals( eventDataValueD.getValue(), eventDataValues.get( 1 ).getValue() );
        assertEquals( eventDataValueE.getValue(), eventDataValues.get( 2 ).getValue() );
    }
}