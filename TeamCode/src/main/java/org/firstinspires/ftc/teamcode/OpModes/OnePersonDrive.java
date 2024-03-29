package org.firstinspires.ftc.teamcode.OpModes;

import com.outoftheboxrobotics.photoncore.PhotonCore;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;

@TeleOp (name = "One Person Drive", group = "Drive")
public class OnePersonDrive extends LinearOpMode {

    private DcMotorEx leftFront, leftRear, rightFront, rightRear, strafeEncoder, leftLift, rightLift, arm, liftMotor;

    private final String setLiftMotor = "leftLift";

    private boolean clawButtonPressed, rotateButtonPressed, liftInMotion;

    private Servo rotate, claw;

    private int lastLiftPosition, lastArmPosition;

    private long startTime;

    private final long LIFT_TIME_OUT = 1500;
    private final double ROTATE_UPSIDE = RobotConstants.ROTATE_UPSIDE, ROTATE_DOWNSIDE = RobotConstants.ROTATE_DOWNSIDE, CLAW_OPEN = RobotConstants.CLAW_OPEN, CLAW_CLOSE = RobotConstants.CLAW_CLOSE, LIFT_ERROR = RobotConstants.LIFT_ERROR;

    private final int TALL = RobotConstants.TALL, MEDIUM = RobotConstants.MEDIUM, LOW = RobotConstants.LOW, CONE_STACK = RobotConstants.CONE_STACK, ARM_FLIPPED = RobotConstants.ARM_FLIPPED, ARM_SHORT = RobotConstants.ARM_SHORT, LIFT_VELOCITY = RobotConstants.LIFT_VELOCITY, ARM_VELOCITY = RobotConstants.ARM_VELOCITY, LIFT_MAXIMUM = RobotConstants.LIFT_MAXIMUM, LIFT_MINIMUM = RobotConstants.LIFT_MINIMUM;
    /**
     * IMPORTANT NOTES:
     *
     * For the Double Shock controllers:
     * triangle - top
     * circle - right
     * cross (X-shaped) - bottom
     * square - left
     *
     * For the older Logitech controllers:
     * Y - top
     * B - right
     * A - bottom
     * X - left
     */

    @Override
    public void runOpMode()
    {
        PhotonCore.enable();

        leftFront = hardwareMap.get(DcMotorEx.class, "leftFront");
        leftRear = hardwareMap.get(DcMotorEx.class, "leftRear");
        rightRear = hardwareMap.get(DcMotorEx.class, "rightRear");
        rightFront = hardwareMap.get(DcMotorEx.class, "rightFront");
        leftLift = hardwareMap.get(DcMotorEx.class, "leftLift");
        rightLift = hardwareMap.get(DcMotorEx.class, "rightLift");
        strafeEncoder = hardwareMap.get(DcMotorEx.class, "strafeEncoder");
        arm = hardwareMap.get(DcMotorEx.class, "arm");
        leftFront.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        leftRear.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightFront.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightRear.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        leftLift.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightLift.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        arm.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightFront.setDirection(DcMotorSimple.Direction.REVERSE);
        rightRear.setDirection(DcMotorSimple.Direction.REVERSE);
        //leftLift.setDirection(DcMotorSimple.Direction.REVERSE);
        rightLift.setDirection(DcMotorSimple.Direction.REVERSE);
        //arm.setDirection(DcMotorSimple.Direction.REVERSE);
        leftFront.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightRear.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        strafeEncoder.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        arm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftLift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightLift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftFront.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rightRear.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        strafeEncoder.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        arm.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        leftLift.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rightLift.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        switch (setLiftMotor)
        {
            case "leftLift":
                liftMotor = leftLift;
                break;
            case "rightLift":
                liftMotor = rightLift;
                break;
        }

        rotate = hardwareMap.get(Servo.class, "rotate");
        claw = hardwareMap.get(Servo.class, "claw");
        rotate.setPosition(ROTATE_UPSIDE);
        claw.setPosition(CLAW_OPEN);

        waitForStart();

        /**
         * left stick forward/backward and turn
         * bumpers strafe
         *
         */
        while (opModeIsActive())
        {
            double y =  gamepad1.left_stick_y; // Remember, this is reversed!
            double rx = -gamepad1.left_stick_x * 1; // Counteract imperfect strafing
            double x = 0; if (gamepad1.right_bumper) {x = -1;} if (gamepad1.left_bumper) {x = 1;}

            // Denominator is the largest motor power (absolute value) or 1
            // This ensures all the powers maintain the same ratio, but only when
            // at least one is out of the range [-1, 1]
            double denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1);
            double leftFrontPower = (y + x + rx) / denominator;
            double leftRearPower = (y - x + rx) / denominator;
            double rightFrontPower = (y - x - rx) / denominator;
            double rightRearPower = (y + x - rx) / denominator;

            leftFront.setPower(leftFrontPower);
            leftRear.setPower(leftRearPower);
            rightFront.setPower(rightFrontPower);
            rightRear.setPower(rightRearPower);

            doClaw();

            doRotate();

            /*
            arm presets:
            dpad_up - tall pole
            dpad_left - medium pole
            dpad_down - short pole
            dpad_right - reset
             */

            if (gamepad1.x)
            {
                liftInMotion = false;
            }

            if (!liftInMotion)
            {
                if (gamepad1.right_trigger>0) {
                    arm.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                    arm.setPower(gamepad1.right_trigger*0.5);
                    lastArmPosition = arm.getCurrentPosition();
                } else if (gamepad1.left_trigger>0) {
                    arm.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                    arm.setPower(-gamepad1.left_trigger*0.5);
                    lastArmPosition = arm.getCurrentPosition();
                } else {
                    arm.setTargetPosition(lastArmPosition);
                    arm.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    arm.setVelocity(ARM_VELOCITY/4);
                }

                if ((liftMotor.getPower()>0&&liftMotor.getCurrentPosition()>LIFT_MAXIMUM)||(liftMotor.getPower()<0&&liftMotor.getCurrentPosition()<LIFT_MINIMUM)) {
                    leftLift.setPower(0);
                    rightLift.setPower(0);
                } else {
                    if (gamepad1.right_stick_y!=0) {
                        leftLift.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                        leftLift.setPower(-gamepad1.right_stick_y);
                        lastLiftPosition = liftMotor.getCurrentPosition();
                        rightLift.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                        rightLift.setPower(-gamepad1.right_stick_y);
                        lastLiftPosition = rightLift.getCurrentPosition();
                    } else {
                        leftLift.setTargetPosition(lastLiftPosition);
                        leftLift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                        leftLift.setVelocity(1440 * 2);
                        rightLift.setTargetPosition(lastLiftPosition);
                        rightLift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                        rightLift.setVelocity(1440 * 2);
                    }
                }
                if (gamepad1.dpad_up)
                {
                    startPreset(TALL,ARM_FLIPPED,ROTATE_DOWNSIDE);
                }
                if (gamepad1.dpad_left)
                {
                    startPreset(MEDIUM,ARM_FLIPPED,ROTATE_DOWNSIDE);
                }
                if (gamepad1.dpad_down)
                {
                    startPreset(LOW,ARM_SHORT,ROTATE_UPSIDE);
                }
                if (gamepad1.dpad_right)
                {
                    startPreset(0,0,ROTATE_UPSIDE);
                }
            } else {
                if (!arm.isBusy()&&!leftLift.isBusy()&&!rightLift.isBusy()) {
                    arm.setPower(0);
                    arm.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                    leftLift.setPower(0);
                    leftLift.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                    rightLift.setPower(0);
                    rightLift.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                    lastLiftPosition = liftMotor.getCurrentPosition();
                    lastArmPosition = arm.getCurrentPosition();
                    liftInMotion = false;
                }
                if (System.currentTimeMillis()-startTime>LIFT_TIME_OUT) {
                    arm.setPower(0);
                    arm.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                    leftLift.setPower(0);
                    leftLift.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                    rightLift.setPower(0);
                    rightLift.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                    lastLiftPosition = liftMotor.getCurrentPosition();
                    lastArmPosition = arm.getCurrentPosition();
                    liftInMotion = false;
                }
            }

            telemetry.addData("claw button pressed", clawButtonPressed);
            telemetry.addData("left lift position", leftLift.getCurrentPosition());
            telemetry.addData("left lift power", leftLift.getPower());
            telemetry.addData("right lift position", rightLift.getCurrentPosition());
            telemetry.addData("right lift power", rightLift.getPower());
            telemetry.addData("lift position", liftMotor.getCurrentPosition());
            telemetry.addData("last lift position", lastLiftPosition);
            telemetry.addData("arm position", arm.getCurrentPosition());
            telemetry.addData("arm power", arm.getPower());
            telemetry.addData("lift in motion", liftInMotion);
            telemetry.addData("left front position", leftFront.getCurrentPosition());
            telemetry.addData("right front position", rightFront.getCurrentPosition());
            telemetry.addData("left rear position", leftRear.getCurrentPosition());
            telemetry.addData("right rear position", rightRear.getCurrentPosition());
            telemetry.addData("left encoder", leftFront.getCurrentPosition());
            telemetry.addData("right encoder", rightRear.getCurrentPosition());
            telemetry.addData("strafe encoder", strafeEncoder.getCurrentPosition());
            telemetry.update();
        }
    }

    public void startPreset(int liftPosition, int armPosition, double rotatePosition) {
        liftInMotion = true;
        leftLift.setTargetPosition(liftPosition);
        leftLift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftLift.setVelocity(LIFT_VELOCITY);
        rightLift.setTargetPosition(liftPosition);
        rightLift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightLift.setVelocity(LIFT_VELOCITY);
        arm.setTargetPosition(armPosition);
        arm.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        arm.setVelocity(ARM_VELOCITY);
        rotate.setPosition(rotatePosition);
        startTime = System.currentTimeMillis();
    }

    public void doClaw() {
        if (gamepad1.a)
        {
            if (clawButtonPressed)
            {
            }
            else
            {
                clawButtonPressed = true;
                if (claw.getPosition()==CLAW_CLOSE)
                {
                    claw.setPosition(CLAW_OPEN);
                }
                else
                {
                    claw.setPosition(CLAW_CLOSE);
                }
            }
        }
        else
        {
            if (clawButtonPressed)
            {
                clawButtonPressed = false;
            }
        }
    }

    public void doRotate() {
        if (gamepad1.b)
        {
            if (rotateButtonPressed)
            {
            }
            else
            {
                rotateButtonPressed = true;
                if (rotate.getPosition()==ROTATE_UPSIDE)
                {
                    rotate.setPosition(ROTATE_DOWNSIDE);
                }
                else
                {
                    rotate.setPosition(ROTATE_UPSIDE);
                }
            }
        }
        else
        {
            if (rotateButtonPressed)
            {
                rotateButtonPressed = false;
            }
        }
    }
}

/**
 * 8==D
 */