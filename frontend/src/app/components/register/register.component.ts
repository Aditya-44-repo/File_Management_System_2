import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit {
  loading = false;
  hidePassword = true;
  hideConfirmPassword = true;

  form = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: [
      '',
      [
        Validators.required,
        Validators.minLength(10),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{10,}$/)
      ]
    ]
    ,
    confirmPassword: [
      '',
      [
        Validators.required,
        Validators.minLength(10),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{10,}$/)
      ]
    ]
  });

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router, private snack: MatSnackBar) {}

  // add group-level validator for password match
  ngOnInit() {
    // attach validator after initialization so `this` is available
    this.form.setValidators(this.passwordMatchValidator.bind(this));
  }

  get passwordValue(): string {
    return this.form.controls.password.value ?? '';
  }

  get hasMinLength(): boolean {
    return this.passwordValue.length >= 10;
  }

  get hasUppercase(): boolean {
    return /[A-Z]/.test(this.passwordValue);
  }

  get hasLowercase(): boolean {
    return /[a-z]/.test(this.passwordValue);
  }

  get hasNumber(): boolean {
    return /\d/.test(this.passwordValue);
  }

  get hasSymbol(): boolean {
    return /[^A-Za-z\d]/.test(this.passwordValue);
  }

  submit() {
    if (this.form.invalid) return;
    this.loading = true;
    const { name, email, password } = this.form.value as any;
    this.auth.register({ name, email, password }).subscribe({
      next: () => {
        this.snack.open('Account created!', 'OK', { duration: 2000 });
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.snack.open(err?.error?.message ?? 'Registration failed', 'Dismiss', { duration: 3500 });
        this.loading = false;
      }
    });
  }

  private passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const password = group.get('password')?.value;
    const confirm = group.get('confirmPassword')?.value;
    return password === confirm ? null : { passwordMismatch: true };
  }
}